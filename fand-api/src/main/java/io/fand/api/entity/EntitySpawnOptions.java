package io.fand.api.entity;

import io.fand.api.world.Vector3;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Optional state applied immediately after an entity is created by a world API.
 *
 * <p>Fields that do not apply to the spawned entity type are ignored. For
 * example, projectile options only affect {@link Projectile} entities and item
 * lifetime options only affect {@link ItemEntity} entities.
 */
public record EntitySpawnOptions(
        @Nullable Vector3 velocity,
        @Nullable Entity projectileShooter,
        @Nullable Vector3 projectileDirection,
        @Nullable Double projectilePower,
        @Nullable Double projectileUncertainty,
        @Nullable Boolean persistent,
        @Nullable Boolean noAi,
        @Nullable LivingEntity target,
        @Nullable Integer pickupDelay,
        boolean unlimitedLifetime,
        @Nullable Component customName,
        @Nullable Boolean customNameVisible,
        @Nullable Boolean glowing,
        @Nullable Boolean silent,
        @Nullable Boolean gravity,
        @Nullable Boolean invulnerable,
        @Nullable Integer fireTicks
) {

    private static final EntitySpawnOptions DEFAULTS = builder().build();

    public EntitySpawnOptions {
        requireFinite(velocity, "velocity");
        requireFinite(projectileDirection, "projectileDirection");
        if (projectilePower != null && (!Double.isFinite(projectilePower) || projectilePower < 0.0)) {
            throw new IllegalArgumentException("projectilePower must be finite and >= 0");
        }
        if (projectileUncertainty != null && (!Double.isFinite(projectileUncertainty) || projectileUncertainty < 0.0)) {
            throw new IllegalArgumentException("projectileUncertainty must be finite and >= 0");
        }
        if (pickupDelay != null && pickupDelay < 0) {
            throw new IllegalArgumentException("pickupDelay must be >= 0");
        }
        if (fireTicks != null && fireTicks < 0) {
            throw new IllegalArgumentException("fireTicks must be >= 0");
        }
    }

    private static void requireFinite(@Nullable Vector3 vector, String name) {
        if (vector != null
                && (!Double.isFinite(vector.x()) || !Double.isFinite(vector.y()) || !Double.isFinite(vector.z()))) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    public static EntitySpawnOptions defaults() {
        return DEFAULTS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .velocity(velocity)
                .projectileShooter(projectileShooter)
                .projectile(projectileDirection, projectilePower, projectileUncertainty)
                .persistent(persistent)
                .noAi(noAi)
                .target(target)
                .pickupDelay(pickupDelay)
                .unlimitedLifetime(unlimitedLifetime)
                .customName(customName)
                .customNameVisible(customNameVisible)
                .glowing(glowing)
                .silent(silent)
                .gravity(gravity)
                .invulnerable(invulnerable)
                .fireTicks(fireTicks);
    }

    public static final class Builder {

        private @Nullable Vector3 velocity;
        private @Nullable Entity projectileShooter;
        private @Nullable Vector3 projectileDirection;
        private @Nullable Double projectilePower;
        private @Nullable Double projectileUncertainty;
        private @Nullable Boolean persistent;
        private @Nullable Boolean noAi;
        private @Nullable LivingEntity target;
        private @Nullable Integer pickupDelay;
        private boolean unlimitedLifetime;
        private @Nullable Component customName;
        private @Nullable Boolean customNameVisible;
        private @Nullable Boolean glowing;
        private @Nullable Boolean silent;
        private @Nullable Boolean gravity;
        private @Nullable Boolean invulnerable;
        private @Nullable Integer fireTicks;

        private Builder() {
        }

        public Builder velocity(@Nullable Vector3 velocity) {
            this.velocity = velocity;
            return this;
        }

        public Builder projectileShooter(@Nullable Entity shooter) {
            this.projectileShooter = shooter;
            return this;
        }

        public Builder projectile(Vector3 direction, double power, double uncertainty) {
            return projectile(Objects.requireNonNull(direction, "direction"), Double.valueOf(power), Double.valueOf(uncertainty));
        }

        public Builder projectile(@Nullable Vector3 direction, @Nullable Double power, @Nullable Double uncertainty) {
            this.projectileDirection = direction;
            this.projectilePower = power;
            this.projectileUncertainty = uncertainty;
            return this;
        }

        public Builder persistent(@Nullable Boolean persistent) {
            this.persistent = persistent;
            return this;
        }

        public Builder noAi(@Nullable Boolean noAi) {
            this.noAi = noAi;
            return this;
        }

        public Builder target(@Nullable LivingEntity target) {
            this.target = target;
            return this;
        }

        public Builder pickupDelay(@Nullable Integer ticks) {
            this.pickupDelay = ticks;
            return this;
        }

        public Builder unlimitedLifetime(boolean unlimitedLifetime) {
            this.unlimitedLifetime = unlimitedLifetime;
            return this;
        }

        public Builder customName(@Nullable Component customName) {
            this.customName = customName;
            return this;
        }

        public Builder customNameVisible(@Nullable Boolean visible) {
            this.customNameVisible = visible;
            return this;
        }

        public Builder glowing(@Nullable Boolean glowing) {
            this.glowing = glowing;
            return this;
        }

        public Builder silent(@Nullable Boolean silent) {
            this.silent = silent;
            return this;
        }

        public Builder gravity(@Nullable Boolean gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder invulnerable(@Nullable Boolean invulnerable) {
            this.invulnerable = invulnerable;
            return this;
        }

        public Builder fireTicks(@Nullable Integer fireTicks) {
            this.fireTicks = fireTicks;
            return this;
        }

        public EntitySpawnOptions build() {
            return new EntitySpawnOptions(
                    velocity,
                    projectileShooter,
                    projectileDirection,
                    projectilePower,
                    projectileUncertainty,
                    persistent,
                    noAi,
                    target,
                    pickupDelay,
                    unlimitedLifetime,
                    customName,
                    customNameVisible,
                    glowing,
                    silent,
                    gravity,
                    invulnerable,
                    fireTicks
            );
        }
    }
}
