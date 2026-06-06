package io.fand.server.entity;

import io.fand.api.permission.PermissionService;
import io.fand.server.world.FandWorld;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Caches FandPlayer wrappers so listeners observe a stable identity per
 * connected player and equality by uuid is consistent across events.
 *
 * <p>Maintains a volatile immutable snapshot list rebuilt only on attach/detach,
 * so broadcast paths (Server.audiences, World.players) reuse it without copying.
 */
public final class PlayerRegistry {

    private final PermissionService permissions;
    private final ConcurrentHashMap<UUID, FandPlayer> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FandPlayer> byName = new ConcurrentHashMap<>();
    private volatile List<FandPlayer> snapshot = List.of();
    private volatile @Nullable Function<ServerLevel, FandWorld> worldResolver;

    public PlayerRegistry(PermissionService permissions) {
        this.permissions = permissions;
    }

    public void bindWorldResolver(Function<ServerLevel, FandWorld> resolver) {
        this.worldResolver = resolver;
    }

    public synchronized FandPlayer attach(ServerPlayer handle) {
        var existing = byId.get(handle.getUUID());
        if (existing != null) {
            byName.put(existing.name(), existing);
            return existing;
        }
        var player = new FandPlayer(handle, permissions, this);
        byId.put(handle.getUUID(), player);
        byName.put(player.name(), player);
        snapshot = List.copyOf(byId.values());
        return player;
    }

    /**
     * Updates the cached FandPlayer to point at the freshly created ServerPlayer that
     * PlayerList.respawn produces. Called from the patched respawn path so listeners
     * keep observing the same FandPlayer identity across deaths and dimension swaps.
     */
    public synchronized FandPlayer onRespawn(ServerPlayer newHandle) {
        var existing = byId.get(newHandle.getUUID());
        if (existing == null) {
            return attach(newHandle);
        }
        existing.refreshHandle(newHandle);
        byName.put(existing.name(), existing);
        return existing;
    }

    public synchronized Optional<FandPlayer> detach(UUID uniqueId) {
        var removed = byId.remove(uniqueId);
        if (removed != null) {
            byName.remove(removed.name(), removed);
            removed.clearTransientState();
            snapshot = List.copyOf(byId.values());
        }
        return Optional.ofNullable(removed);
    }

    public Optional<FandPlayer> find(UUID uniqueId) {
        return Optional.ofNullable(byId.get(uniqueId));
    }

    public Optional<FandPlayer> findByName(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public Collection<FandPlayer> snapshot() {
        return snapshot;
    }

    FandWorld wrapLevel(ServerLevel level) {
        var resolver = this.worldResolver;
        return resolver != null ? resolver.apply(level) : new FandWorld(level);
    }
}
