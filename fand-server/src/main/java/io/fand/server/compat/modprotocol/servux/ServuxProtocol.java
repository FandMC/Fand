package io.fand.server.compat.modprotocol.servux;

import io.fand.api.Fand;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.player.PlayerQuitEvent;
import io.fand.api.messaging.PluginMessageDirection;
import io.fand.api.messaging.PluginMessageRegistration;
import io.fand.server.config.FandConfig;
import io.fand.server.entity.FandPlayer;
import io.fand.server.event.EventDispatcher;
import io.fand.server.messaging.FandPluginMessaging;
import io.fand.server.structure.FandStructureService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServuxProtocol implements AutoCloseable {

    static final String VERSION_STRING = "Fand Servux compatibility";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServuxProtocol.class);
    private static final long[] METADATA_RETRY_TICKS = {1L, 5L, 20L};

    private final FandPluginMessaging messaging;
    private final ConfigView config;
    private final ServuxSender sender;
    private final ServuxSplitter splitter = new ServuxSplitter();
    private final ServuxHudProvider hud;
    private final ServuxEntityLikeProvider entities;
    private final ServuxEntityLikeProvider tweaks;
    private final ServuxStructureProvider structures;
    private final ServuxLitematicaProvider litematica;
    private final List<PluginMessageRegistration> registrations = new ArrayList<>();
    private final Map<UUID, List<String>> loggers = new HashMap<>();
    private final EventSubscription joinSubscription;
    private final EventSubscription quitSubscription;

    public ServuxProtocol(
            FandPluginMessaging messaging,
            EventDispatcher events,
            FandStructureService structures,
            FandConfig.Servux config
    ) {
        this.messaging = Objects.requireNonNull(messaging, "messaging");
        this.config = new ConfigView(Objects.requireNonNull(config, "config"));
        this.sender = new ServuxSender(messaging);
        this.hud = new ServuxHudProvider(this, this.config);
        this.entities = new ServuxEntityLikeProvider(
                this,
                this.config,
                ServuxChannels.ENTITIES,
                "entity_data",
                1,
                false);
        this.tweaks = new ServuxEntityLikeProvider(
                this,
                this.config,
                ServuxChannels.TWEAKS,
                "tweaks_data",
                1,
                true);
        this.structures = new ServuxStructureProvider(this, this.config);
        this.litematica = new ServuxLitematicaProvider(this, this.config, structures);
        registerChannels();
        this.joinSubscription = events.subscribe(PlayerJoinEvent.class, EventPriority.OBSERVER, this::sendMetadataOnJoin);
        this.quitSubscription = events.subscribe(PlayerQuitEvent.class, EventPriority.OBSERVER, event -> forget(event.player().uniqueId()));
    }

    public void tick(MinecraftServer server) {
        int tick = server.getTickCount();
        hud.tick(server, tick);
        structures.tick(server, tick);
    }

    public ServuxHudProvider hud() {
        return hud;
    }

    boolean hasLogger(ServerPlayer player) {
        return !loggers(player).isEmpty();
    }

    List<String> loggers(ServerPlayer player) {
        return loggers.getOrDefault(player.getUUID(), List.of());
    }

    void setLogger(ServerPlayer player, List<String> enabled) {
        if (enabled.isEmpty()) {
            loggers.remove(player.getUUID());
            return;
        }
        loggers.put(player.getUUID(), List.copyOf(enabled));
    }

    void send(ServerPlayer player, Key channel, byte[] payload) {
        sender.send(player, channel, payload);
    }

    void sendSplit(ServerPlayer player, Key channel, int transactionId, CompoundTag tag) {
        sendSplit(player, channel, transactionId, tag, ServuxPacketType.S2C_NBT_RESPONSE_DATA.id());
    }

    void sendSplit(ServerPlayer player, Key channel, int transactionId, CompoundTag tag, int dataPacketType) {
        byte[] splitPayload = channel.equals(ServuxChannels.LITEMATICA)
                || channel.equals(ServuxChannels.ENTITIES)
                || channel.equals(ServuxChannels.TWEAKS)
                ? ServuxPacketCodec.writeTransactionalSplitPayload(transactionId, tag)
                : ServuxPacketCodec.writeSplitPayload(tag);
        splitter.send(channel, splitPayload, dataPacketType, (targetChannel, payload) -> send(player, targetChannel, payload));
    }

    void forget(UUID player) {
        loggers.remove(player);
        splitter.forget(player);
        structures.forget(player);
        litematica.forget(player);
    }

    @Override
    public void close() {
        joinSubscription.close();
        quitSubscription.close();
        for (var registration : registrations) {
            registration.close();
        }
        registrations.clear();
        loggers.clear();
        splitter.clear();
        structures.clear();
        litematica.close();
    }

    private void registerChannels() {
        registrations.add(messaging.register(ServuxChannels.HUD, PluginMessageDirection.BIDIRECTIONAL, this::handleHud));
        registrations.add(messaging.register(ServuxChannels.ENTITIES, PluginMessageDirection.BIDIRECTIONAL, this::handleEntities));
        registrations.add(messaging.register(ServuxChannels.STRUCTURES, PluginMessageDirection.BIDIRECTIONAL, this::handleStructures));
        registrations.add(messaging.register(ServuxChannels.LITEMATICA, PluginMessageDirection.BIDIRECTIONAL, this::handleLitematica));
        registrations.add(messaging.register(ServuxChannels.TWEAKS, PluginMessageDirection.BIDIRECTIONAL, this::handleTweaks));
    }

    private void handleHud(io.fand.api.entity.Player apiPlayer, io.fand.api.messaging.PluginMessageChannel channel, byte[] payload) {
        decode(apiPlayer, payload, hud::handle);
    }

    private void handleEntities(io.fand.api.entity.Player apiPlayer, io.fand.api.messaging.PluginMessageChannel channel, byte[] payload) {
        decode(apiPlayer, payload, entities::handle);
    }

    private void handleStructures(io.fand.api.entity.Player apiPlayer, io.fand.api.messaging.PluginMessageChannel channel, byte[] payload) {
        decode(apiPlayer, payload, structures::handle);
    }

    private void handleLitematica(io.fand.api.entity.Player apiPlayer, io.fand.api.messaging.PluginMessageChannel channel, byte[] payload) {
        if (!(apiPlayer instanceof FandPlayer fandPlayer)) {
            return;
        }
        var packet = ServuxPacketCodec.read(payload);
        if (packet != null) {
            runSafely(fandPlayer.handle(), () -> litematica.handle(fandPlayer.handle(), packet));
        }
    }

    private void handleTweaks(io.fand.api.entity.Player apiPlayer, io.fand.api.messaging.PluginMessageChannel channel, byte[] payload) {
        decode(apiPlayer, payload, tweaks::handle);
    }

    private void decode(
            io.fand.api.entity.Player apiPlayer,
            byte[] payload,
            java.util.function.BiConsumer<ServerPlayer, ServuxPacketCodec.Incoming> handler
    ) {
        if (!(apiPlayer instanceof FandPlayer fandPlayer)) {
            return;
        }
        var packet = ServuxPacketCodec.read(payload);
        if (packet != null) {
            runSafely(fandPlayer.handle(), () -> handler.accept(fandPlayer.handle(), packet));
        }
    }

    private void runSafely(ServerPlayer player, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            LOGGER.debug("Servux packet handling failed for {}", player.getGameProfile().name(), failure);
        }
    }

    private void sendMetadataOnJoin(PlayerJoinEvent event) {
        if (!(event.player() instanceof FandPlayer player)) {
            return;
        }
        sendMetadata(player.handle());
        var uuid = player.uniqueId();
        var server = player.handle().level().getServer();
        for (long delayTicks : METADATA_RETRY_TICKS) {
            Fand.server().scheduler().runMainAfterTicks(() -> {
                var live = server.getPlayerList().getPlayer(uuid);
                if (live != null) {
                    sendMetadata(live);
                }
            }, delayTicks);
        }
    }

    private void sendMetadata(ServerPlayer player) {
        hud.sendMetadata(player);
        entities.sendMetadata(player);
        structures.sendMetadata(player);
        litematica.sendMetadata(player);
        tweaks.sendMetadata(player);
    }

    static final class ConfigView implements
            ServuxHudProvider.FandConfigView,
            ServuxStructureProvider.FandConfigView {

        private final FandConfig.Servux config;
        private final Set<String> structureWhitelist;
        private final Set<String> structureBlacklist;

        private ConfigView(FandConfig.Servux config) {
            this.config = config;
            this.structureWhitelist = parseCsvSet(config.structureWhitelist);
            this.structureBlacklist = parseCsvSet(config.structureBlacklist);
        }

        boolean entityDataEnabled() {
            return config.entityData;
        }

        int entityPermissionLevel() {
            return config.entityPermissionLevel;
        }

        boolean tweaksEnabled() {
            return config.tweaks;
        }

        int tweaksPermissionLevel() {
            return config.tweaksPermissionLevel;
        }

        boolean allowPlayerInventory() {
            return config.playerInventory;
        }

        int playerInventoryPermissionLevel() {
            return config.playerInventoryPermissionLevel;
        }

        boolean allowPlayerEnderItems() {
            return config.playerEnderItems;
        }

        int playerEnderItemsPermissionLevel() {
            return config.playerEnderItemsPermissionLevel;
        }

        boolean stackableShulkers() {
            return config.stackableShulkers;
        }

        int stackableShulkerSize() {
            return Math.max(1, config.stackableShulkerSize);
        }

        boolean litematicaEnabled() {
            return config.litematica;
        }

        int litematicaPermissionLevel() {
            return config.litematicaPermissionLevel;
        }

        boolean litematicaPasteEnabled() {
            return config.litematicaPaste;
        }

        int litematicaPastePermissionLevel() {
            return config.litematicaPastePermissionLevel;
        }

        @Override
        public boolean hudEnabled() {
            return config.hud;
        }

        @Override
        public int hudPermissionLevel() {
            return config.hudPermissionLevel;
        }

        @Override
        public int hudUpdateInterval() {
            return Math.max(1, config.hudUpdateIntervalTicks);
        }

        @Override
        public boolean hudLoggersEnabled() {
            return config.hudLoggers;
        }

        @Override
        public int hudLoggerPermissionLevel() {
            return config.hudLoggerPermissionLevel;
        }

        @Override
        public boolean shareWeather() {
            return config.shareWeather;
        }

        @Override
        public int weatherPermissionLevel() {
            return config.weatherPermissionLevel;
        }

        @Override
        public boolean shareSeed() {
            return config.shareSeed;
        }

        @Override
        public int seedPermissionLevel() {
            return config.seedPermissionLevel;
        }

        @Override
        public boolean structuresEnabled() {
            return config.structures;
        }

        @Override
        public int structuresPermissionLevel() {
            return config.structuresPermissionLevel;
        }

        @Override
        public int structuresUpdateInterval() {
            return Math.max(1, config.structuresUpdateIntervalTicks);
        }

        @Override
        public int structuresTimeoutTicks() {
            return Math.max(1, config.structuresTimeoutTicks);
        }

        @Override
        public boolean structureWhitelistEnabled() {
            return config.structureWhitelistEnabled;
        }

        @Override
        public Set<String> structureWhitelist() {
            return structureWhitelist;
        }

        @Override
        public boolean structureBlacklistEnabled() {
            return config.structureBlacklistEnabled;
        }

        @Override
        public Set<String> structureBlacklist() {
            return structureBlacklist;
        }

        private static Set<String> parseCsvSet(String value) {
            if (value == null || value.isBlank()) {
                return Collections.emptySet();
            }
            var parsed = new HashSet<String>();
            Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(entry -> !entry.isEmpty())
                    .map(entry -> entry.toLowerCase(Locale.ROOT))
                    .forEach(parsed::add);
            return Set.copyOf(parsed);
        }
    }
}
