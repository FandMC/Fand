package io.fand.server.entity;

import io.fand.api.permission.PermissionService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Caches FandPlayer wrappers so listeners observe a stable identity per
 * connected player and equality by uuid is consistent across events.
 */
public final class PlayerRegistry {

    private final PermissionService permissions;
    private final ConcurrentHashMap<UUID, FandPlayer> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FandPlayer> byName = new ConcurrentHashMap<>();

    public PlayerRegistry(PermissionService permissions) {
        this.permissions = permissions;
    }

    public FandPlayer attach(ServerPlayer handle) {
        var player = byId.computeIfAbsent(handle.getUUID(), id -> new FandPlayer(handle, permissions));
        byName.put(player.name(), player);
        return player;
    }

    public Optional<FandPlayer> detach(UUID uniqueId) {
        var removed = byId.remove(uniqueId);
        if (removed != null) {
            byName.remove(removed.name(), removed);
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
        return List.copyOf(byId.values());
    }
}
