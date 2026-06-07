package io.fand.server.entity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fand.api.entity.Entity;
import io.fand.server.world.WorldRegistry;
import java.time.Duration;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;

/**
 * Caches {@link FandLivingEntity} wrappers around vanilla non-player living entities
 * so listeners observe a stable identity across consecutive event fires for the same
 * mob.
 *
 * <p>Players are cached in {@link PlayerRegistry} instead — those have lifecycle
 * hooks (join/quit/respawn). Mobs do not, so entries here expire after idle access:
 * a wrapper survives as long as something keeps firing events for that UUID.
 */
public final class EntityRegistry {

    private final WorldRegistry worldRegistry;
    private final PlayerRegistry players;
    private final Cache<UUID, FandEntity> entityWrappers = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(8192)
            .build();
    private final Cache<UUID, FandLivingEntity> livingWrappers = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(8192)
            .build();

    public EntityRegistry(WorldRegistry worldRegistry, PlayerRegistry players) {
        this.worldRegistry = worldRegistry;
        this.players = players;
    }

    public Entity wrap(net.minecraft.world.entity.Entity handle) {
        if (handle instanceof net.minecraft.server.level.ServerPlayer player) {
            var wrapped = players.findOrNull(player.getUUID());
            if (wrapped != null) {
                return wrapped;
            }
        }
        if (handle instanceof LivingEntity living) {
            return wrap(living);
        }
        var existing = entityWrappers.getIfPresent(handle.getUUID());
        if (existing != null && existing.handle() == handle) {
            return existing;
        }
        var fresh = new FandEntity(handle, worldRegistry);
        entityWrappers.put(handle.getUUID(), fresh);
        return fresh;
    }

    public FandLivingEntity wrap(LivingEntity handle) {
        var existing = livingWrappers.getIfPresent(handle.getUUID());
        if (existing != null && existing.handle() == handle) {
            return existing;
        }
        var fresh = new FandLivingEntity(handle, worldRegistry);
        livingWrappers.put(handle.getUUID(), fresh);
        return fresh;
    }
}
