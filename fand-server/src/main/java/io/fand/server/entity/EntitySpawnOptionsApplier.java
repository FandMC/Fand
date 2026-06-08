package io.fand.server.entity;

import io.fand.api.entity.EntitySpawnOptions;
import java.util.Objects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class EntitySpawnOptionsApplier {

    private static final double DEFAULT_PROJECTILE_POWER = 1.5;
    private static final double DEFAULT_PROJECTILE_UNCERTAINTY = 0.0;

    private EntitySpawnOptionsApplier() {
    }

    public static void apply(net.minecraft.world.entity.Entity entity, EntitySpawnOptions options) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(options, "options");

        applyCommon(entity, options);
        if (entity instanceof Mob mob) {
            applyMob(mob, options);
        }
        if (entity instanceof Projectile projectile) {
            applyProjectile(projectile, options);
        }
        if (entity instanceof ItemEntity item) {
            applyItem(item, options);
        }
    }

    private static void applyCommon(net.minecraft.world.entity.Entity entity, EntitySpawnOptions options) {
        if (options.velocity() != null) {
            var velocity = options.velocity();
            entity.setDeltaMovement(new Vec3(velocity.x(), velocity.y(), velocity.z()));
        }
        if (options.customName() != null) {
            EntityStates.setCustomName(entity, options.customName());
        }
        if (options.customNameVisible() != null) {
            EntityStates.setCustomNameVisible(entity, options.customNameVisible());
        }
        if (options.glowing() != null) {
            EntityStates.setGlowing(entity, options.glowing());
        }
        if (options.silent() != null) {
            EntityStates.setSilent(entity, options.silent());
        }
        if (options.gravity() != null) {
            EntityStates.setGravity(entity, options.gravity());
        }
        if (options.invulnerable() != null) {
            EntityStates.setInvulnerable(entity, options.invulnerable());
        }
        if (options.fireTicks() != null) {
            entity.setRemainingFireTicks(options.fireTicks());
        }
    }

    private static void applyMob(Mob mob, EntitySpawnOptions options) {
        if (Boolean.TRUE.equals(options.persistent())) {
            mob.setPersistenceRequired();
        }
        if (options.noAi() != null) {
            mob.setNoAi(options.noAi());
        }
        if (options.target() != null) {
            mob.setTarget((net.minecraft.world.entity.LivingEntity) EntityHandles.unwrap(options.target()));
        }
    }

    private static void applyProjectile(Projectile projectile, EntitySpawnOptions options) {
        if (options.projectileShooter() != null) {
            projectile.setOwner(EntityHandles.unwrap(options.projectileShooter()));
        }
        if (options.projectileDirection() != null) {
            var direction = options.projectileDirection();
            projectile.shoot(
                    direction.x(),
                    direction.y(),
                    direction.z(),
                    (float) valueOr(options.projectilePower(), DEFAULT_PROJECTILE_POWER),
                    (float) valueOr(options.projectileUncertainty(), DEFAULT_PROJECTILE_UNCERTAINTY));
        }
    }

    private static void applyItem(ItemEntity item, EntitySpawnOptions options) {
        if (options.pickupDelay() != null) {
            item.setPickUpDelay(options.pickupDelay());
        }
        if (options.unlimitedLifetime()) {
            item.setUnlimitedLifetime();
        }
    }

    private static double valueOr(Double value, double fallback) {
        return value == null ? fallback : value;
    }
}
