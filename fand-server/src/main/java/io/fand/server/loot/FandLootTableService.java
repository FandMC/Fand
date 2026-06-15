package io.fand.server.loot;

import io.fand.api.item.ItemStack;
import io.fand.api.loot.LootGenerator;
import io.fand.api.loot.LootContext;
import io.fand.api.loot.LootTableRegistration;
import io.fand.api.loot.LootTableService;
import io.fand.api.loot.LootTableView;
import io.fand.api.world.Location;
import io.fand.server.hooks.FandHooks;
import io.fand.server.entity.FandPlayer;
import io.fand.server.item.FandItemStacks;
import io.fand.server.world.FandWorld;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class FandLootTableService implements LootTableService {

    private final Supplier<MinecraftServer> server;
    private final Object lock = new Object();
    private final LinkedHashMap<Key, Replacement> replacements = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public FandLootTableService(Supplier<MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public Optional<LootTableView> table(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            if (replacements.containsKey(key)) {
                return Optional.of(new LootTableView(key));
            }
        }
        var current = server.get();
        if (current == null) {
            return Optional.empty();
        }
        var resourceKey = lootTableKey(key);
        return current.reloadableRegistries().lookup()
                .lookup(Registries.LOOT_TABLE)
                .flatMap(registry -> registry.get(resourceKey))
                .map(ignored -> new LootTableView(key));
    }

    @Override
    public List<ItemStack> generate(Key key, LootContext context) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(context, "context");
        var replacement = replacement(key);
        if (replacement != null) {
            return List.copyOf(replacement.generator().generate(context));
        }
        var current = requireServer();
        return callOnServerThread(current, () -> generateOnServerThread(current, key, context));
    }

    @Override
    public LootTableRegistration replace(Key key, LootGenerator generator) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(generator, "generator");
        var token = sequence.incrementAndGet();
        synchronized (lock) {
            replacements.put(key, new Replacement(token, generator));
        }
        return new Registration(this, key, token);
    }

    public @org.jspecify.annotations.Nullable ObjectArrayList<net.minecraft.world.item.ItemStack> generateVanilla(
            net.minecraft.resources.ResourceKey<LootTable> key,
            LootParams params
    ) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(params, "params");
        var replacement = replacement(key(key.identifier()));
        if (replacement == null) {
            return null;
        }
        var generated = replacement.generator().generate(context(params));
        var result = new ObjectArrayList<net.minecraft.world.item.ItemStack>();
        for (var item : generated) {
            var vanilla = FandItemStacks.toVanilla(item);
            if (!vanilla.isEmpty()) {
                result.add(vanilla);
            }
        }
        return result;
    }

    private static LootContext context(LootParams params) {
        var level = params.getLevel();
        var world = FandHooks.wrapWorld(level);
        Location location = null;
        var origin = params.contextMap().getOptional(LootContextParams.ORIGIN);
        if (world != null && origin != null) {
            location = new Location(world, origin.x(), origin.y(), origin.z(), 0.0F, 0.0F);
        }
        var player = Optional.ofNullable(params.contextMap().getOptional(LootContextParams.LAST_DAMAGE_PLAYER))
                .or(() -> Optional.ofNullable(params.contextMap().getOptional(LootContextParams.ATTACKING_ENTITY))
                        .filter(net.minecraft.world.entity.player.Player.class::isInstance)
                        .map(net.minecraft.world.entity.player.Player.class::cast))
                .or(() -> Optional.ofNullable(params.contextMap().getOptional(LootContextParams.THIS_ENTITY))
                        .filter(net.minecraft.world.entity.player.Player.class::isInstance)
                        .map(net.minecraft.world.entity.player.Player.class::cast))
                .flatMap(entity -> Optional.ofNullable(FandHooks.wrapEntity(entity)))
                .orElse(null);
        return new LootContext(location, player, params.getLuck());
    }

    private List<ItemStack> generateOnServerThread(MinecraftServer current, Key key, LootContext context) {
        var resourceKey = lootTableKey(key);
        var table = current.reloadableRegistries().lookup()
                .lookup(Registries.LOOT_TABLE)
                .flatMap(registry -> registry.get(resourceKey))
                .map(holder -> holder.value())
                .orElse(null);
        if (table == null || table == LootTable.EMPTY) {
            return List.of();
        }

        var level = level(current, context);
        var builder = builder(level, context);
        if (!canCreate(table, builder)) {
            return List.of();
        }
        if (context.luck() != 0.0F) {
            builder.withLuck(context.luck());
        }
        return table.getRandomItems(builder.create(table.getParamSet())).stream()
                .map(FandItemStacks::fromVanilla)
                .toList();
    }

    private static LootParams.Builder builder(ServerLevel level, LootContext context) {
        var builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, origin(context));
        context.killerOptional()
                .filter(FandPlayer.class::isInstance)
                .map(FandPlayer.class::cast)
                .map(FandPlayer::handle)
                .ifPresent(player -> {
                    builder.withParameter(LootContextParams.THIS_ENTITY, player);
                    builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
                    builder.withParameter(LootContextParams.ATTACKING_ENTITY, player);
                    builder.withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player);
                    builder.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player));
                });
        return builder;
    }

    private static boolean canCreate(LootTable table, LootParams.Builder builder) {
        return table.getParamSet().required().stream()
                .allMatch(param -> builder.getOptionalParameter(param) != null);
    }

    private static ServerLevel level(MinecraftServer server, LootContext context) {
        return context.locationOptional()
                .map(location -> {
                    if (location.world() instanceof FandWorld world) {
                        return world.handle();
                    }
                    var id = Identifier.fromNamespaceAndPath(location.world().key().namespace(), location.world().key().value());
                    return server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
                })
                .orElseGet(server::overworld);
    }

    private static Vec3 origin(LootContext context) {
        return context.locationOptional()
                .map(location -> new Vec3(location.x(), location.y(), location.z()))
                .orElse(Vec3.ZERO);
    }

    private static ResourceKey<LootTable> lootTableKey(Key key) {
        return ResourceKey.create(Registries.LOOT_TABLE, identifier(key));
    }

    private Replacement replacement(Key key) {
        synchronized (lock) {
            return replacements.get(key);
        }
    }

    private boolean active(Key key, long token) {
        synchronized (lock) {
            var replacement = replacements.get(key);
            return replacement != null && replacement.token() == token;
        }
    }

    private boolean remove(Key key, long token) {
        synchronized (lock) {
            var replacement = replacements.get(key);
            if (replacement == null || replacement.token() != token) {
                return false;
            }
            replacements.remove(key);
            return true;
        }
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static Key key(Identifier id) {
        return Key.key(id.getNamespace(), id.getPath());
    }

    private MinecraftServer requireServer() {
        var current = server.get();
        if (current == null) {
            throw new IllegalStateException("Minecraft server is not attached");
        }
        return current;
    }

    private static <T> T callOnServerThread(MinecraftServer server, Supplier<T> task) {
        if (server.isSameThread()) {
            return task.get();
        }
        return server.submit(task::get).join();
    }

    private record Replacement(long token, LootGenerator generator) {
    }

    private static final class Registration implements LootTableRegistration {

        private final FandLootTableService owner;
        private final Key key;
        private final long token;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private Registration(FandLootTableService owner, Key key, long token) {
            this.owner = owner;
            this.key = key;
            this.token = token;
        }

        @Override
        public Key key() {
            return key;
        }

        @Override
        public boolean active() {
            return active.get() && owner.active(key, token);
        }

        @Override
        public void unregister() {
            if (active.compareAndSet(true, false)) {
                owner.remove(key, token);
            }
        }
    }
}
