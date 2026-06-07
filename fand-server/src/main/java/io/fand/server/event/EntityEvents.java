package io.fand.server.event;

import io.fand.api.event.entity.EntityRemoveEvent;
import io.fand.api.event.entity.EntitySpawnEvent;
import io.fand.api.event.entity.EntityTeleportEvent;
import io.fand.api.event.entity.EntityDeathEvent;
import io.fand.api.event.entity.ExplosionPrimeEvent;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.server.hooks.FandHooks;
import io.fand.server.world.FandWorld;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntityEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityEvents.class);

    private EntityEvents() {
    }

    public static void fireDeath(net.minecraft.world.entity.LivingEntity entity, DamageSource source) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityDeathEvent.class)) {
            return;
        }
        var fandEntity = FandHooks.wrapLivingEntity(entity);
        if (fandEntity == null) {
            return;
        }
        var directEntity = Optional.ofNullable(source.getDirectEntity())
                .filter(candidate -> candidate instanceof net.minecraft.world.entity.LivingEntity)
                .map(candidate -> FandHooks.wrapLivingEntity((net.minecraft.world.entity.LivingEntity) candidate));
        var attacker = Optional.ofNullable(source.getEntity())
                .filter(candidate -> candidate instanceof net.minecraft.world.entity.LivingEntity)
                .map(candidate -> FandHooks.wrapLivingEntity((net.minecraft.world.entity.LivingEntity) candidate));
        var cause = source.typeHolder().unwrapKey().map(key -> key.identifier().toString()).orElse("minecraft:generic");
        try {
            bus.fire(new EntityDeathEvent(fandEntity, cause, directEntity, attacker));
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityDeathEvent listener failed", failure);
        }
    }

    public static boolean fireSpawn(net.minecraft.world.entity.Entity entity, EntitySpawnEvent.Cause cause) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntitySpawnEvent.class)) {
            return true;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        if (fandEntity == null) {
            return true;
        }
        var event = new EntitySpawnEvent(fandEntity, cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntitySpawnEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static void fireRemove(net.minecraft.world.entity.Entity entity) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityRemoveEvent.class)) {
            return;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        if (fandEntity == null) {
            return;
        }
        try {
            bus.fire(new EntityRemoveEvent(fandEntity, removeCause(entity.getRemovalReason())));
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityRemoveEvent listener failed", failure);
        }
    }

    public static @Nullable TeleportTransition fireTeleport(
            net.minecraft.world.entity.Entity entity,
            TeleportTransition transition
    ) {
        if (entity instanceof net.minecraft.server.level.ServerPlayer) {
            return transition;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityTeleportEvent.class)) {
            return transition;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        var fromWorld = entity.level() instanceof ServerLevel serverLevel ? FandHooks.wrapWorld(serverLevel) : null;
        var toWorld = FandHooks.wrapWorld(transition.newLevel());
        if (fandEntity == null || fromWorld == null || toWorld == null) {
            return transition;
        }
        PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(
                PositionMoveRotation.of(entity),
                PositionMoveRotation.of(transition),
                transition.relatives());
        var from = new Location(fromWorld, entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        var to = new Location(
                toWorld,
                absolute.position().x,
                absolute.position().y,
                absolute.position().z,
                absolute.yRot(),
                absolute.xRot());
        var event = new EntityTeleportEvent(fandEntity, from, to, teleportCause(entity.level(), transition.newLevel()));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityTeleportEvent listener failed", failure);
            return transition;
        }
        if (event.cancelled()) {
            return null;
        }
        if (sameLocation(to, event.to())) {
            return transition;
        }
        ServerLevel level = resolveLevel(event.to().world(), transition.newLevel());
        if (level == null) {
            LOGGER.warn("EntityTeleportEvent targeted an unloaded world: {}", event.to().world().key().asString());
            return transition;
        }
        return new TeleportTransition(
                level,
                new Vec3(event.to().x(), event.to().y(), event.to().z()),
                transition.deltaMovement(),
                event.to().yaw(),
                event.to().pitch(),
                transition.missingRespawnBlock(),
                transition.asPassenger(),
                java.util.Set.of(),
                transition.postTeleportTransition());
    }

    public static @Nullable ExplosionResult fireExplosionPrime(
            ServerLevel level,
            net.minecraft.world.entity.@Nullable Entity source,
            double x,
            double y,
            double z,
            float radius,
            boolean fire
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(ExplosionPrimeEvent.class)) {
            return new ExplosionResult(radius, fire);
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return new ExplosionResult(radius, fire);
        }
        var event = new ExplosionPrimeEvent(
                new Location(world, x, y, z, 0.0F, 0.0F),
                Optional.ofNullable(source).map(FandHooks::wrapEntity),
                radius,
                fire);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("ExplosionPrimeEvent listener failed", failure);
            return new ExplosionResult(radius, fire);
        }
        return event.cancelled() ? null : new ExplosionResult(event.radius(), event.fire());
    }

    private static EntityRemoveEvent.Cause removeCause(net.minecraft.world.entity.Entity.@Nullable RemovalReason reason) {
        if (reason == null) {
            return EntityRemoveEvent.Cause.UNKNOWN;
        }
        return switch (reason) {
            case KILLED -> EntityRemoveEvent.Cause.KILLED;
            case DISCARDED -> EntityRemoveEvent.Cause.DISCARDED;
            case UNLOADED_TO_CHUNK -> EntityRemoveEvent.Cause.UNLOADED_TO_CHUNK;
            case UNLOADED_WITH_PLAYER -> EntityRemoveEvent.Cause.UNLOADED_WITH_PLAYER;
            case CHANGED_DIMENSION -> EntityRemoveEvent.Cause.CHANGED_DIMENSION;
        };
    }

    private static EntityTeleportEvent.Cause teleportCause(
            net.minecraft.world.level.Level from,
            ServerLevel to
    ) {
        return from.dimension() == to.dimension()
                ? EntityTeleportEvent.Cause.UNKNOWN
                : EntityTeleportEvent.Cause.DIMENSION_CHANGE;
    }

    private static @Nullable ServerLevel resolveLevel(World world, ServerLevel fallback) {
        if (world instanceof FandWorld fandWorld) {
            return fandWorld.handle();
        }
        var server = fallback.getServer();
        if (server == null) {
            return null;
        }
        var key = world.key();
        for (var level : server.getAllLevels()) {
            var identifier = level.dimension().identifier();
            if (identifier.getNamespace().equals(key.namespace()) && identifier.getPath().equals(key.value())) {
                return level;
            }
        }
        return null;
    }

    private static boolean sameLocation(Location a, Location b) {
        return a.world().equals(b.world())
                && Double.compare(a.x(), b.x()) == 0
                && Double.compare(a.y(), b.y()) == 0
                && Double.compare(a.z(), b.z()) == 0
                && Float.compare(a.yaw(), b.yaw()) == 0
                && Float.compare(a.pitch(), b.pitch()) == 0;
    }

    public record ExplosionResult(float radius, boolean fire) {
    }
}
