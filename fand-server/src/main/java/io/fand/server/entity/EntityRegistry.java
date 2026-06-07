package io.fand.server.entity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fand.server.world.WorldRegistry;
import java.time.Duration;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;

/**
 * Caches API wrappers around vanilla non-player entities so listeners observe a
 * stable identity across consecutive event fires for the same entity.
 *
 * <p>Players are cached in {@link PlayerRegistry} instead — those have lifecycle
 * hooks (join/quit/respawn). Mobs do not, so entries here expire after idle access:
 * a wrapper survives as long as something keeps firing events for that UUID.
 */
public final class EntityRegistry {

    private final WorldRegistry worldRegistry;
    private final Cache<UUID, FandEntity> wrappers = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(8192)
            .build();

    public EntityRegistry(WorldRegistry worldRegistry) {
        this.worldRegistry = worldRegistry;
    }

    public FandEntity wrap(net.minecraft.world.entity.Entity handle) {
        var existing = wrappers.getIfPresent(handle.getUUID());
        if (existing != null && existing.handle() == handle) {
            return existing;
        }
        var fresh = handle instanceof net.minecraft.world.entity.LivingEntity living
                ? new FandLivingEntity(living, worldRegistry)
                : new FandEntity(handle, worldRegistry);
        wrappers.put(handle.getUUID(), fresh);
        return fresh;
    }

    public FandLivingEntity wrap(LivingEntity handle) {
        var wrapped = wrap((net.minecraft.world.entity.Entity) handle);
        if (wrapped instanceof FandLivingEntity living) {
            return living;
        }
        throw new IllegalStateException("Living entity wrapped as non-living entity: " + handle);
    }
}
