package io.fand.server.world;

import io.fand.api.world.World;
import io.fand.server.entity.PlayerRegistry;
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
 * so identity is stable across calls. Reads from {@link MinecraftServer#getAllLevels()}
 * lazily; entries that no longer exist are evicted on the next snapshot.
 */
public final class WorldRegistry {

    private final MinecraftServer server;
    private final PlayerRegistry players;
    private final ConcurrentHashMap<Key, FandWorld> byKey = new ConcurrentHashMap<>();

    public WorldRegistry(MinecraftServer server, PlayerRegistry players) {
        this.server = server;
        this.players = players;
    }

    public Collection<World> snapshot() {
        var current = new ArrayList<World>();
        var seen = new java.util.HashSet<Key>();
        for (var level : server.getAllLevels()) {
            var world = wrap(level);
            current.add(world);
            seen.add(world.key());
        }
        byKey.keySet().retainAll(seen);
        return List.copyOf(current);
    }

    public Optional<World> find(Key key) {
        var cached = byKey.get(key);
        if (cached != null) {
            return Optional.of(cached);
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
        var identifier = level.dimension().identifier();
        var key = Key.key(identifier.getNamespace(), identifier.getPath());
        var existing = byKey.get(key);
        if (existing != null && existing.handle() == level) {
            return existing;
        }
        return byKey.compute(key, (ignored, current) -> {
            if (current != null && current.handle() == level) {
                return current;
            }
            return new FandWorld(level, players);
        });
    }
}
