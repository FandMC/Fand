package io.fand.server.compat.modprotocol.recipeviewer;

import io.fand.api.Fand;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.player.PlayerQuitEvent;
import io.fand.server.entity.FandPlayer;
import io.fand.server.event.EventDispatcher;
import io.fand.server.messaging.FandPluginMessaging;
import io.fand.server.messaging.FandPluginMessaging.ConfigurationMessageRegistration;
import io.netty.buffer.Unpooled;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FabricRecipeSyncProtocol implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricRecipeSyncProtocol.class);
    private static final Key SUPPORTED_SERIALIZERS = Key.key("fabric:recipe_sync/supported_serializers");
    private static final Identifier RECIPE_SYNC = Identifier.fromNamespaceAndPath("fabric", "recipe_sync");
    private static final int VANILLA_CUSTOM_PAYLOAD_LIMIT = 1_048_576;
    private static final long[] JOIN_SYNC_RETRY_TICKS = {1L, 5L, 20L};
    // Capped/bounded caches: a client can send supported_serializers during
    // configuration and disconnect before reaching PLAY (where PlayerQuitEvent
    // would forget it). expiry reaps those orphan entries so a misbehaving or
    // hostile client cannot grow this map without bound.
    private static final Duration SERIALIZER_CACHE_TTL = Duration.ofMinutes(10);

    private final EventSubscription joinSubscription;
    private final EventSubscription quitSubscription;
    private final ConfigurationMessageRegistration serializersRegistration;
    private final Cache<UUID, Set<Identifier>> supportedSerializers = Caffeine.newBuilder()
            .expireAfterWrite(SERIALIZER_CACHE_TTL)
            .maximumSize(8192)
            .build();
    private final Cache<String, Set<Identifier>> supportedSerializersByName = Caffeine.newBuilder()
            .expireAfterWrite(SERIALIZER_CACHE_TTL)
            .maximumSize(8192)
            .build();

    public FabricRecipeSyncProtocol(FandPluginMessaging messaging, EventDispatcher events) {
        this.serializersRegistration = messaging.registerConfiguration(SUPPORTED_SERIALIZERS, this::handleSupportedSerializers);
        this.joinSubscription = events.subscribe(PlayerJoinEvent.class, EventPriority.OBSERVER, this::syncOnJoin);
        this.quitSubscription = events.subscribe(PlayerQuitEvent.class, EventPriority.OBSERVER, this::forgetOnQuit);
    }

    private void handleSupportedSerializers(ServerConfigurationPacketListenerImpl listener, byte[] payload) {
        var serializers = readSerializers(payload);
        if (!serializers.isEmpty()) {
            supportedSerializers.put(listener.getOwner().id(), serializers);
            supportedSerializersByName.put(listener.getOwner().name().toLowerCase(Locale.ROOT), serializers);
        }
    }

    private void syncOnJoin(PlayerJoinEvent event) {
        if (!(event.player() instanceof FandPlayer player)) {
            return;
        }
        sync(player.handle());
        for (long delayTicks : JOIN_SYNC_RETRY_TICKS) {
            Fand.server().scheduler().runMainAfterTicks(() -> sync(player.handle()), delayTicks);
        }
    }

    private void forgetOnQuit(PlayerQuitEvent event) {
        supportedSerializers.invalidate(event.player().uniqueId());
        supportedSerializersByName.invalidate(event.player().name().toLowerCase(Locale.ROOT));
    }

    public void syncDataPackContents(ServerPlayer player, boolean joined) {
        sync(player);
    }

    private void sync(ServerPlayer player) {
        if (player.connection == null) {
            return;
        }
        try {
            byte[] payload = encodePayload(player);
            if (payload.length == 0) {
                return;
            }
            if (payload.length > VANILLA_CUSTOM_PAYLOAD_LIMIT) {
                LOGGER.warn(
                        "Skipping Fabric recipe sync for {} because payload is {} bytes, above vanilla custom payload limit {}",
                        player.getGameProfile().name(),
                        payload.length,
                        VANILLA_CUSTOM_PAYLOAD_LIMIT);
                return;
            }
            player.connection.send(new ClientboundCustomPayloadPacket(new DiscardedPayload(RECIPE_SYNC, payload)));
        } catch (Throwable failure) {
            LOGGER.warn("Fabric recipe sync failed for {}", player.getGameProfile().name(), failure);
        }
    }

    byte[] encodePayload(ServerPlayer player) {
        var entries = groupedRecipes(player);
        if (entries.isEmpty()) {
            return new byte[0];
        }
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buffer.writeVarInt(entries.size());
        for (var entry : entries) {
            Identifier serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(entry.serializer());
            if (serializerId == null) {
                continue;
            }
            buffer.writeIdentifier(serializerId);
            buffer.writeVarInt(entry.recipes().size());
            var codec = entry.serializer().streamCodec();
            for (var holder : entry.recipes()) {
                buffer.writeResourceKey(holder.id());
                encodeRecipe(codec, buffer, holder.value());
            }
        }
        return readBytes(buffer);
    }

    private List<Entry> groupedRecipes(ServerPlayer player) {
        Set<Identifier> supported = supported(player);
        if (supported == null || supported.isEmpty()) {
            return List.of();
        }
        var grouped = new IdentityHashMap<RecipeSerializer<?>, List<RecipeHolder<?>>>();
        for (var holder : player.level().getServer().getRecipeManager().getRecipes()) {
            RecipeSerializer<?> serializer = holder.value().getSerializer();
            Identifier serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer);
            if (!shouldSync(serializerId, supported)) {
                continue;
            }
            grouped.computeIfAbsent(serializer, ignored -> new ArrayList<>()).add(holder);
        }
        return grouped.entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), entry.getValue().stream()
                        .sorted(Comparator.comparing(holder -> holder.id().identifier().toString()))
                        .toList()))
                .sorted(Comparator.comparing(entry -> BuiltInRegistries.RECIPE_SERIALIZER.getKey(entry.serializer()).toString()))
                .toList();
    }

    private Set<Identifier> supported(ServerPlayer player) {
        Set<Identifier> supported = supportedSerializers.getIfPresent(player.getUUID());
        if (supported != null) {
            return supported;
        }
        return supportedSerializersByName.getIfPresent(player.getGameProfile().name().toLowerCase(Locale.ROOT));
    }

    private static boolean shouldSync(Identifier serializerId, Set<Identifier> supported) {
        if (serializerId == null) {
            return false;
        }
        return supported.contains(serializerId);
    }

    private static Set<Identifier> readSerializers(byte[] payload) {
        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload));
        int size = Math.min(buffer.readVarInt(), 1024);
        var serializers = new LinkedHashSet<Identifier>(size);
        for (int i = 0; i < size; i++) {
            if (!buffer.isReadable(1)) {
                break;
            }
            serializers.add(buffer.readIdentifier());
        }
        return serializers;
    }

    private static byte[] readBytes(FriendlyByteBuf buffer) {
        var bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void encodeRecipe(
            net.minecraft.network.codec.StreamCodec codec,
            RegistryFriendlyByteBuf buffer,
            Recipe<?> recipe
    ) {
        codec.encode(buffer, recipe);
    }

    @Override
    public void close() {
        serializersRegistration.close();
        joinSubscription.close();
        quitSubscription.close();
        supportedSerializers.invalidateAll();
        supportedSerializersByName.invalidateAll();
    }

    private record Entry(RecipeSerializer<?> serializer, List<RecipeHolder<?>> recipes) {
    }
}
