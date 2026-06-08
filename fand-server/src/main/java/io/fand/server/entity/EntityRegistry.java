package io.fand.server.entity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fand.server.world.WorldRegistry;
import java.time.Duration;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Caches API wrappers around vanilla entities so listeners observe a stable
 * identity across consecutive event fires for the same entity.
 *
 * <p>Players are delegated to {@link PlayerRegistry}; non-player entries expire
 * after idle access because they do not have explicit attach/detach hooks here.
 */
public final class EntityRegistry {

    private final WorldRegistry worldRegistry;
    private final PlayerRegistry players;
    private final Cache<UUID, FandEntity> wrappers = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(8192)
            .build();

    public EntityRegistry(WorldRegistry worldRegistry, PlayerRegistry players) {
        this.worldRegistry = worldRegistry;
        this.players = players;
    }

    public io.fand.api.entity.Entity wrap(net.minecraft.world.entity.Entity handle) {
        if (handle instanceof ServerPlayer player) {
            var existing = players.findOrNull(player.getUUID());
            return existing != null ? existing : players.attach(player);
        }
        var existing = wrappers.getIfPresent(handle.getUUID());
        if (existing != null && existing.handle() == handle) {
            return existing;
        }
        var fresh = wrapFresh(handle);
        wrappers.put(handle.getUUID(), fresh);
        return fresh;
    }

    public io.fand.api.entity.LivingEntity wrap(LivingEntity handle) {
        var wrapped = wrap((net.minecraft.world.entity.Entity) handle);
        if (wrapped instanceof io.fand.api.entity.LivingEntity living) {
            return living;
        }
        throw new IllegalStateException("Living entity wrapped as non-living entity: " + handle);
    }

    private FandEntity wrapFresh(net.minecraft.world.entity.Entity handle) {
        if (handle instanceof net.minecraft.world.entity.item.ItemEntity item) {
            return new FandItemEntity(item, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            return new FandProjectile(projectile, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.TamableAnimal tameable) {
            return new FandTameable(tameable, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.animal.Animal animal) {
            return new FandAnimal(animal, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.AgeableMob ageable) {
            return new FandAgeable(ageable, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.Mob mob) {
            return new FandMob(mob, worldRegistry);
        }
        if (handle instanceof net.minecraft.world.entity.LivingEntity living) {
            return new FandLivingEntity(living, worldRegistry);
        }
        return new FandEntity(handle, worldRegistry);
    }
}
