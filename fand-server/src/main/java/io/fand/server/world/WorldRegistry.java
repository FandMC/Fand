package io.fand.server.world;

import io.fand.api.world.World;
import io.fand.server.entity.EntityRegistry;
import io.fand.server.entity.PlayerRegistry;
import io.fand.server.scheduler.TaskScheduler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Caches {@link FandWorld} wrappers around vanilla {@link ServerLevel} instances
 * so identity is stable across calls.
 */
public final class WorldRegistry {

    private final MinecraftServer server;
    private final PlayerRegistry players;
    private final TaskScheduler scheduler;
    private final EntityRegistry entities;
    private final ConcurrentHashMap<ServerLevel, FandWorld> byLevel = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Key, FandWorld> byKey = new ConcurrentHashMap<>();

    public WorldRegistry(MinecraftServer server, PlayerRegistry players, TaskScheduler scheduler) {
        this.server = server;
        this.players = players;
        this.scheduler = scheduler;
        this.entities = new EntityRegistry(this, players);
    }

    public EntityRegistry entityRegistry() {
        return entities;
    }

    public Collection<World> snapshot() {
        var current = new ArrayList<World>();
        for (var level : server.getAllLevels()) {
            var world = wrap(level);
            current.add(world);
        }
        return List.copyOf(current);
    }

    public Optional<World> find(Key key) {
        var cached = byKey.get(key);
        if (cached != null && server.getLevel(cached.handle().dimension()) == cached.handle()) {
            return Optional.of(cached);
        } else if (cached != null) {
            byKey.remove(key, cached);
            byLevel.remove(cached.handle(), cached);
        }
        for (var level : server.getAllLevels()) {
            var identifier = level.dimension().identifier();
            var levelKey = Key.key(identifier.getNamespace(), identifier.getPath());
            if (levelKey.equals(key)) {
                return Optional.of(wrap(level));
            }
        }
        return Optional.empty();
    }

    public Optional<World> defaultWorld() {
        var overworld = server.overworld();
        return overworld == null ? Optional.empty() : Optional.of(wrap(overworld));
    }

    public FandWorld wrap(ServerLevel level) {
        var existing = byLevel.get(level);
        if (existing != null) {
            byKey.putIfAbsent(existing.key(), existing);
            return existing;
        }
        return byLevel.computeIfAbsent(level, current -> {
            var world = new FandWorld(current, players, this, scheduler);
            byKey.put(world.key(), world);
            return world;
        });
    }

    public void forget(FandWorld world) {
        byLevel.remove(world.handle(), world);
        byKey.remove(world.key(), world);
    }
}
