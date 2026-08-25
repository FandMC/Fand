package io.fand.server.entity;

import io.fand.api.permission.PermissionService;
import io.fand.server.scoreboard.FandScoreboardService;
import io.fand.server.tablist.FandTabListService;
import io.fand.server.world.FandWorld;
import io.fand.server.world.WorldRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final FandScoreboardService scoreboards;
    private final FandTabListService tabLists;
    private final ConcurrentHashMap<UUID, FandPlayer> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FandPlayer> byName = new ConcurrentHashMap<>();
    private volatile Map<ServerLevel, List<FandPlayer>> snapshotsByLevel = Map.of();
    private volatile List<FandPlayer> snapshot = List.of();
    private volatile @Nullable WorldRegistry worldRegistry;

    public PlayerRegistry(
            PermissionService permissions,
            FandScoreboardService scoreboards,
            FandTabListService tabLists
    ) {
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.scoreboards = Objects.requireNonNull(scoreboards, "scoreboards");
        this.tabLists = Objects.requireNonNull(tabLists, "tabLists");
    }

    public synchronized void bindWorldRegistry(WorldRegistry worldRegistry) {
        Objects.requireNonNull(worldRegistry, "worldRegistry");
        if (this.worldRegistry != null) {
            throw new IllegalStateException("World registry is already bound");
        }
        this.worldRegistry = worldRegistry;
    }

    WorldRegistry worldRegistry() {
        var current = worldRegistry;
        if (current == null) {
            throw new IllegalStateException("World registry is not bound");
        }
        return current;
    }

    public synchronized FandPlayer attach(ServerPlayer handle) {
        var existing = byId.get(handle.getUUID());
        if (existing != null) {
            rebindName(existing);
            return existing;
        }
        var player = new FandPlayer(handle, permissions, this, scoreboards, tabLists);
        byId.put(handle.getUUID(), player);
        byName.put(player.name(), player);
        rebuildSnapshots();
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
        rebindName(existing);
        rebuildSnapshots();
        return existing;
    }

    public synchronized Optional<FandPlayer> detach(UUID uniqueId) {
        var removed = byId.remove(uniqueId);
        if (removed != null) {
            byName.values().removeIf(player -> player == removed);
            removed.clearTransientState();
            tabLists.clearPlayer(uniqueId);
            rebuildSnapshots();
        }
        return Optional.ofNullable(removed);
    }

    // Drop any stale name→player mapping for this player before binding the
    // current name, so a rename (proxy/imposter name swap) does not leave a
    // dangling old key that findByName would resolve to the wrong identity.
    private void rebindName(FandPlayer player) {
        byName.values().removeIf(existing -> existing == player);
        byName.put(player.name(), player);
    }

    public Optional<FandPlayer> find(UUID uniqueId) {
        return Optional.ofNullable(findOrNull(uniqueId));
    }

    public @Nullable FandPlayer findOrNull(UUID uniqueId) {
        return byId.get(uniqueId);
    }

    public Optional<FandPlayer> findByName(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public Collection<FandPlayer> snapshot() {
        return snapshot;
    }

    public Collection<FandPlayer> snapshot(ServerLevel level) {
        return snapshotsByLevel.getOrDefault(level, List.of());
    }

    /**
     * Rebuilds the per-level snapshots after a player switched dimensions, so
     * {@code World.players()} and per-world broadcasts stop attributing the
     * player to the previous level. Called from the patched
     * {@code ServerPlayer.teleport} dimension-change path via
     * {@code PlayerEvents.fireChangedWorld}, which runs regardless of whether
     * any listener is registered.
     */
    public synchronized void onChangedWorld(ServerPlayer handle) {
        if (byId.containsKey(handle.getUUID())) {
            rebuildSnapshots();
        }
    }

    synchronized void refreshSnapshots() {
        rebuildSnapshots();
    }

    FandWorld wrapLevel(ServerLevel level) {
        return worldRegistry().wrap(level);
    }

    private void rebuildSnapshots() {
        var current = List.copyOf(byId.values());
        var grouped = new HashMap<ServerLevel, List<FandPlayer>>();
        for (var player : current) {
            grouped.computeIfAbsent(player.handle().level(), ignored -> new ArrayList<>()).add(player);
        }
        grouped.replaceAll((level, players) -> List.copyOf(players));
        // Publish complete replacements so concurrent readers never observe a
        // half-built view (the previous clear-then-put left a window where
        // broadcasts saw an empty level list).
        snapshotsByLevel = Map.copyOf(grouped);
        snapshot = current;
    }
}
