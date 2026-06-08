package io.fand.server.event;

import io.fand.api.event.entity.EntityCombustEvent;
import io.fand.api.event.entity.EntityCombustByBlockEvent;
import io.fand.api.event.entity.EntityCombustByEntityEvent;
import io.fand.api.event.entity.EntityBreedEvent;
import io.fand.api.event.entity.EntityChangeBlockEvent;
import io.fand.api.event.entity.EntityDismountEvent;
import io.fand.api.event.entity.EntityDropItemEvent;
import io.fand.api.event.entity.EntityCreatePortalEvent;
import io.fand.api.event.entity.EntityDamageByBlockEvent;
import io.fand.api.event.entity.EntityDamageByEntityEvent;
import io.fand.api.event.entity.EntityDamageEvent;
import io.fand.api.event.entity.EntityExplodeEvent;
import io.fand.api.event.entity.EntityMountEvent;
import io.fand.api.event.entity.EntityPickupItemEvent;
import io.fand.api.event.entity.EntityPortalEvent;
import io.fand.api.event.entity.EntityPortalEnterEvent;
import io.fand.api.event.entity.EntityPortalExitEvent;
import io.fand.api.event.entity.EntityPotionEffectEvent;
import io.fand.api.event.entity.EntityRegainHealthEvent;
import io.fand.api.event.entity.EntityRemoveEvent;
import io.fand.api.event.entity.EntityResurrectEvent;
import io.fand.api.event.entity.EntityShootBowEvent;
import io.fand.api.event.entity.EntitySpawnEvent;
import io.fand.api.event.entity.EntityTameEvent;
import io.fand.api.event.entity.EntityTargetEvent;
import io.fand.api.event.entity.EntityTargetLivingEntityEvent;
import io.fand.api.event.entity.EntityTeleportEvent;
import io.fand.api.event.entity.EntityTransformEvent;
import io.fand.api.event.entity.EntityDeathEvent;
import io.fand.api.event.entity.ExplosionPrimeEvent;
import io.fand.api.event.entity.HangingBreakEvent;
import io.fand.api.event.entity.HangingPlaceEvent;
import io.fand.api.event.entity.ItemDespawnEvent;
import io.fand.api.event.entity.ItemMergeEvent;
import io.fand.api.event.entity.ItemSpawnEvent;
import io.fand.api.event.entity.LingeringPotionSplashEvent;
import io.fand.api.event.entity.PlayerItemFrameChangeEvent;
import io.fand.api.event.entity.PotionSplashEvent;
import io.fand.api.event.entity.ProjectileHitEvent;
import io.fand.api.event.entity.ProjectileLaunchEvent;
import io.fand.api.event.block.BlockFace;
import io.fand.api.event.vehicle.VehicleCreateEvent;
import io.fand.api.event.vehicle.VehicleDestroyEvent;
import io.fand.api.event.vehicle.VehicleEnterEvent;
import io.fand.api.event.vehicle.VehicleExitEvent;
import io.fand.api.event.vehicle.VehicleMoveEvent;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.server.block.FandBlock;
import io.fand.server.block.FandBlockType;
import io.fand.server.entity.FandLivingEntity;
import io.fand.server.entity.FandPlayer;
import io.fand.server.hooks.FandHooks;
import io.fand.server.item.FandItemStacks;
import io.fand.server.world.FandWorld;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntityEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityEvents.class);
    private static final ThreadLocal<EntityRegainHealthEvent.Cause> NEXT_HEAL_CAUSE = new ThreadLocal<>();
    private static final ThreadLocal<EntityTeleportEvent.Cause> NEXT_TELEPORT_CAUSE = new ThreadLocal<>();
    private static final ThreadLocal<BlockPos> NEXT_BLOCK_DAMAGE_SOURCE = new ThreadLocal<>();

    private EntityEvents() {
    }

    public static void withHealCause(EntityRegainHealthEvent.Cause cause, Runnable task) {
        EntityRegainHealthEvent.Cause previous = NEXT_HEAL_CAUSE.get();
        NEXT_HEAL_CAUSE.set(cause);
        try {
            task.run();
        } finally {
            if (previous == null) {
                NEXT_HEAL_CAUSE.remove();
            } else {
                NEXT_HEAL_CAUSE.set(previous);
            }
        }
    }

    public static void withTeleportCause(EntityTeleportEvent.Cause cause, Runnable task) {
        EntityTeleportEvent.Cause previous = NEXT_TELEPORT_CAUSE.get();
        NEXT_TELEPORT_CAUSE.set(Objects.requireNonNull(cause, "cause"));
        try {
            task.run();
        } finally {
            if (previous == null) {
                NEXT_TELEPORT_CAUSE.remove();
            } else {
                NEXT_TELEPORT_CAUSE.set(previous);
            }
        }
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

    public static @Nullable Float fireDamage(
            net.minecraft.world.entity.LivingEntity entity,
            DamageSource source,
            float damage
    ) {
        var bus = FandHooks.events();
        boolean hasGenericListeners = bus.hasListeners(EntityDamageEvent.class);
        boolean hasByEntityListeners = bus.hasListeners(EntityDamageByEntityEvent.class);
        boolean hasByBlockListeners = bus.hasListeners(EntityDamageByBlockEvent.class);
        if (!hasGenericListeners && !hasByEntityListeners && !hasByBlockListeners) {
            return damage;
        }
        var victim = FandHooks.wrapLivingEntity(entity);
        if (victim == null) {
            return damage;
        }
        var directEntity = Optional.ofNullable(source.getDirectEntity())
                .filter(candidate -> candidate instanceof net.minecraft.world.entity.LivingEntity)
                .map(candidate -> FandHooks.wrapLivingEntity((net.minecraft.world.entity.LivingEntity) candidate));
        var attacker = Optional.ofNullable(source.getEntity())
                .filter(candidate -> candidate instanceof net.minecraft.world.entity.LivingEntity)
                .map(candidate -> FandHooks.wrapLivingEntity((net.minecraft.world.entity.LivingEntity) candidate));
        BlockPos explicitBlockSource = NEXT_BLOCK_DAMAGE_SOURCE.get();
        var blockSource = !(entity.level() instanceof ServerLevel serverLevel)
                ? Optional.<io.fand.api.block.Block>empty()
                : Optional.ofNullable(explicitBlockSource)
                        .or(() -> Optional.ofNullable(source.sourcePositionRaw()).map(net.minecraft.core.BlockPos::containing))
                        .flatMap(pos -> Optional.ofNullable(FandHooks.wrapWorld(serverLevel))
                                .map(world -> new FandBlock(world, pos.getX(), pos.getY(), pos.getZ())));
        if (attacker.isEmpty() && blockSource.isEmpty() && !hasGenericListeners) {
            return damage;
        }
        var cause = source.typeHolder().unwrapKey().map(key -> key.identifier().toString()).orElse("minecraft:generic");
        EntityDamageEvent event = attacker
                .<EntityDamageEvent>map(damager -> new EntityDamageByEntityEvent(victim, cause, damage, damager, directEntity))
                .or(() -> blockSource.map(block -> new EntityDamageByBlockEvent(victim, cause, damage, block)))
                .orElseGet(() -> new EntityDamageEvent(victim, cause, damage, directEntity, Optional.empty()));
        FandHooks.fireOrLog(
                bus,
                event,
                event instanceof EntityDamageByEntityEvent
                        ? "EntityDamageByEntityEvent"
                        : event instanceof EntityDamageByBlockEvent ? "EntityDamageByBlockEvent" : "EntityDamageEvent");
        if (event.cancelled()) {
            return null;
        }
        double adjusted = event.amount();
        if (adjusted < 0.0) {
            adjusted = 0.0;
        }
        return (float) adjusted;
    }

    public static boolean withBlockDamageSource(BlockPos sourcePos, java.util.function.BooleanSupplier task) {
        BlockPos previous = NEXT_BLOCK_DAMAGE_SOURCE.get();
        NEXT_BLOCK_DAMAGE_SOURCE.set(Objects.requireNonNull(sourcePos, "sourcePos"));
        try {
            return task.getAsBoolean();
        } finally {
            if (previous == null) {
                NEXT_BLOCK_DAMAGE_SOURCE.remove();
            } else {
                NEXT_BLOCK_DAMAGE_SOURCE.set(previous);
            }
        }
    }

    public static boolean fireSpawn(net.minecraft.world.entity.Entity entity, EntitySpawnEvent.Cause cause) {
        var bus = FandHooks.events();
        boolean hasEntityListeners = bus.hasListeners(EntitySpawnEvent.class);
        boolean hasItemListeners = entity instanceof net.minecraft.world.entity.item.ItemEntity
                && bus.hasListeners(ItemSpawnEvent.class);
        boolean hasVehicleListeners = isVehicle(entity) && bus.hasListeners(VehicleCreateEvent.class);
        if (!hasEntityListeners && !hasItemListeners && !hasVehicleListeners) {
            return true;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        if (fandEntity == null) {
            return true;
        }
        if (hasEntityListeners) {
            var event = new EntitySpawnEvent(fandEntity, cause);
            try {
                bus.fire(event);
            } catch (RuntimeException failure) {
                LOGGER.warn("EntitySpawnEvent listener failed", failure);
                return true;
            }
            if (event.cancelled()) {
                return false;
            }
        }
        if (hasItemListeners && entity instanceof net.minecraft.world.entity.item.ItemEntity itemEntity) {
            var event = new ItemSpawnEvent(fandEntity, FandItemStacks.fromVanilla(itemEntity.getItem()));
            try {
                bus.fire(event);
            } catch (RuntimeException failure) {
                LOGGER.warn("ItemSpawnEvent listener failed", failure);
                return true;
            }
            if (event.cancelled() || event.item().isEmpty()) {
                return false;
            }
            try {
                itemEntity.setItem(FandItemStacks.toVanilla(event.item()));
            } catch (RuntimeException failure) {
                LOGGER.warn("ItemSpawnEvent supplied an invalid item stack", failure);
            }
        }
        if (hasVehicleListeners) {
            var event = new VehicleCreateEvent(fandEntity);
            try {
                bus.fire(event);
            } catch (RuntimeException failure) {
                LOGGER.warn("VehicleCreateEvent listener failed", failure);
                return true;
            }
            if (event.cancelled()) {
                return false;
            }
        }
        return true;
    }

    public static int fireCombust(
            net.minecraft.world.entity.Entity entity,
            int ticks,
            EntityCombustEvent.Cause cause
    ) {
        return fireCombust(entity, ticks, cause, null, null);
    }

    public static int fireCombust(
            net.minecraft.world.entity.Entity entity,
            int ticks,
            EntityCombustEvent.Cause cause,
            net.minecraft.world.entity.@Nullable Entity sourceEntity,
            @Nullable BlockPos sourceBlock
    ) {
        if (ticks <= 0) {
            return ticks;
        }
        var bus = FandHooks.events();
        boolean hasGeneric = bus.hasListeners(EntityCombustEvent.class);
        boolean hasByEntity = sourceEntity != null && bus.hasListeners(EntityCombustByEntityEvent.class);
        boolean hasByBlock = sourceBlock != null && bus.hasListeners(EntityCombustByBlockEvent.class);
        if (!hasGeneric && !hasByEntity && !hasByBlock) {
            return ticks;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        if (fandEntity == null) {
            return ticks;
        }
        EntityCombustEvent event;
        if (sourceEntity != null) {
            var combuster = FandHooks.wrapEntity(sourceEntity);
            if (combuster == null) {
                return ticks;
            }
            event = new EntityCombustByEntityEvent(fandEntity, combuster, cause, ticks / 20.0F);
        } else if (sourceBlock != null && entity.level() instanceof ServerLevel level) {
            var world = FandHooks.wrapWorld(level);
            if (world == null) {
                return ticks;
            }
            event = new EntityCombustByBlockEvent(fandEntity, new FandBlock(world, sourceBlock.getX(), sourceBlock.getY(), sourceBlock.getZ()), cause, ticks / 20.0F);
        } else {
            event = new EntityCombustEvent(fandEntity, Optional.empty(), cause, ticks / 20.0F);
        }
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("{} listener failed", event.getClass().getSimpleName(), failure);
            return ticks;
        }
        return event.cancelled() ? 0 : Math.max(0, Math.round(event.durationSeconds() * 20.0F));
    }

    public static float fireRegainHealth(
            net.minecraft.world.entity.LivingEntity entity,
            float amount,
            EntityRegainHealthEvent.Cause cause
    ) {
        if (amount <= 0.0F) {
            return amount;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityRegainHealthEvent.class)) {
            return amount;
        }
        var fandEntity = FandHooks.wrapLivingEntity(entity);
        if (fandEntity == null) {
            return amount;
        }
        var event = new EntityRegainHealthEvent(fandEntity, amount, cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityRegainHealthEvent listener failed", failure);
            return amount;
        }
        return event.cancelled() ? 0.0F : (float) event.amount();
    }

    public static EntityRegainHealthEvent.Cause currentHealCause() {
        EntityRegainHealthEvent.Cause cause = NEXT_HEAL_CAUSE.get();
        return cause == null ? EntityRegainHealthEvent.Cause.UNKNOWN : cause;
    }

    public static boolean fireMount(net.minecraft.world.entity.Entity entity, net.minecraft.world.entity.Entity vehicle) {
        var bus = FandHooks.events();
        boolean hasMountListeners = bus.hasListeners(EntityMountEvent.class);
        boolean hasVehicleListeners = isVehicle(vehicle) && bus.hasListeners(VehicleEnterEvent.class);
        if (!hasMountListeners && !hasVehicleListeners) {
            return true;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        var fandVehicle = FandHooks.wrapEntity(vehicle);
        if (fandEntity == null || fandVehicle == null) {
            return true;
        }
        if (hasMountListeners) {
            var event = new EntityMountEvent(fandEntity, fandVehicle);
            try {
                bus.fire(event);
            } catch (RuntimeException failure) {
                LOGGER.warn("EntityMountEvent listener failed", failure);
                return true;
            }
            if (event.cancelled()) {
                return false;
            }
        }
        if (hasVehicleListeners) {
            var event = new VehicleEnterEvent(fandVehicle, fandEntity);
            try {
                bus.fire(event);
            } catch (RuntimeException failure) {
                LOGGER.warn("VehicleEnterEvent listener failed", failure);
                return true;
            }
            if (event.cancelled()) {
                return false;
            }
        }
        return true;
    }

    public static boolean fireDismount(net.minecraft.world.entity.Entity entity, net.minecraft.world.entity.Entity vehicle) {
        var bus = FandHooks.events();
        boolean hasDismountListeners = bus.hasListeners(EntityDismountEvent.class);
        boolean hasVehicleListeners = isVehicle(vehicle) && bus.hasListeners(VehicleExitEvent.class);
        if (!hasDismountListeners && !hasVehicleListeners) {
            return true;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        var fandVehicle = FandHooks.wrapEntity(vehicle);
        if (fandEntity == null || fandVehicle == null) {
            return true;
        }
        if (hasDismountListeners) {
            var event = new EntityDismountEvent(fandEntity, fandVehicle);
            try {
                bus.fire(event);
            } catch (RuntimeException failure) {
                LOGGER.warn("EntityDismountEvent listener failed", failure);
                return true;
            }
            if (event.cancelled()) {
                return false;
            }
        }
        if (hasVehicleListeners) {
            var event = new VehicleExitEvent(fandVehicle, fandEntity);
            try {
                bus.fire(event);
            } catch (RuntimeException failure) {
                LOGGER.warn("VehicleExitEvent listener failed", failure);
                return true;
            }
            if (event.cancelled()) {
                return false;
            }
        }
        return true;
    }

    public static boolean fireTame(net.minecraft.world.entity.LivingEntity entity, net.minecraft.server.level.ServerPlayer owner) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityTameEvent.class)) {
            return true;
        }
        var fandEntity = FandHooks.wrapLivingEntity(entity);
        var fandOwner = FandHooks.findPlayer(owner.getUUID());
        if (fandEntity == null || fandOwner == null) {
            return true;
        }
        var event = new EntityTameEvent(fandEntity, fandOwner);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityTameEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireBreed(
            net.minecraft.world.entity.animal.Animal parent,
            net.minecraft.world.entity.animal.Animal partner,
            net.minecraft.world.entity.AgeableMob child
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityBreedEvent.class)) {
            return true;
        }
        var fandParent = FandHooks.wrapLivingEntity(parent);
        var fandPartner = FandHooks.wrapLivingEntity(partner);
        var fandChild = FandHooks.wrapLivingEntity(child);
        if (fandParent == null || fandPartner == null || fandChild == null) {
            return true;
        }
        Optional<io.fand.api.entity.Player> breeder = Optional.ofNullable(parent.getLoveCause())
                .or(() -> Optional.ofNullable(partner.getLoveCause()))
                .map(player -> FandHooks.findPlayer(player.getUUID()));
        var event = new EntityBreedEvent(fandParent, fandPartner, breeder, fandChild);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityBreedEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static TargetResult fireTarget(
            net.minecraft.world.entity.Mob entity,
            net.minecraft.world.entity.@Nullable LivingEntity target,
            EntityTargetEvent.Cause cause
    ) {
        var bus = FandHooks.events();
        boolean hasGeneric = bus.hasListeners(EntityTargetEvent.class);
        boolean hasLiving = target != null && bus.hasListeners(EntityTargetLivingEntityEvent.class);
        if (!hasGeneric && !hasLiving) {
            return new TargetResult(true, target);
        }
        var fandEntity = FandHooks.wrapLivingEntity(entity);
        if (fandEntity == null) {
            return new TargetResult(true, target);
        }
        Optional<io.fand.api.entity.LivingEntity> oldTarget = Optional.ofNullable(entity.getTargetUnchecked())
                .map(FandHooks::wrapLivingEntity);
        Optional<io.fand.api.entity.LivingEntity> newTarget = Optional.ofNullable(target)
                .map(FandHooks::wrapLivingEntity);
        EntityTargetEvent event = newTarget
                .<EntityTargetEvent>map(living -> new EntityTargetLivingEntityEvent(fandEntity, oldTarget, living, cause))
                .orElseGet(() -> new EntityTargetEvent(fandEntity, oldTarget, Optional.empty(), cause));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("{} listener failed", event.getClass().getSimpleName(), failure);
            return new TargetResult(true, target);
        }
        if (event.cancelled()) {
            return new TargetResult(false, target);
        }
        return new TargetResult(true, event.target().map(EntityEvents::toVanillaLiving).orElse(null));
    }

    public static boolean firePotionEffect(
            net.minecraft.world.entity.LivingEntity entity,
            Holder<MobEffect> effect,
            @Nullable MobEffectInstance oldEffect,
            @Nullable MobEffectInstance newEffect,
            net.minecraft.world.entity.@Nullable Entity source,
            EntityPotionEffectEvent.Action action
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityPotionEffectEvent.class)) {
            return true;
        }
        var fandEntity = FandHooks.wrapLivingEntity(entity);
        if (fandEntity == null) {
            return true;
        }
        var identifier = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        var key = identifier == null
                ? net.kyori.adventure.key.Key.key("minecraft:unknown")
                : net.kyori.adventure.key.Key.key(identifier.getNamespace(), identifier.getPath());
        var event = new EntityPotionEffectEvent(
                fandEntity,
                key,
                Optional.ofNullable(oldEffect).map(EntityEvents::effect),
                Optional.ofNullable(newEffect).map(EntityEvents::effect),
                Optional.ofNullable(source).map(FandHooks::wrapEntity),
                action);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityPotionEffectEvent listener failed", failure);
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
        var event = new EntityTeleportEvent(fandEntity, from, to, currentTeleportCause(entity.level(), transition.newLevel()));
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

    public static @Nullable TeleportTransition firePortal(
            net.minecraft.world.entity.Entity entity,
            TeleportTransition transition
    ) {
        if (entity instanceof net.minecraft.server.level.ServerPlayer) {
            return transition;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityPortalEvent.class)) {
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
        var event = new EntityPortalEvent(fandEntity, from, to);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityPortalEvent listener failed", failure);
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
            LOGGER.warn("EntityPortalEvent targeted an unloaded world: {}", event.to().world().key().asString());
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

    public static void firePortalEnter(net.minecraft.world.entity.Entity entity, BlockPos pos) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityPortalEnterEvent.class) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        var world = FandHooks.wrapWorld(level);
        if (fandEntity == null || world == null) {
            return;
        }
        try {
            bus.fire(new EntityPortalEnterEvent(
                    fandEntity,
                    new Location(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, entity.getYRot(), entity.getXRot())));
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityPortalEnterEvent listener failed", failure);
        }
    }

    public static @Nullable TeleportTransition firePortalExit(
            net.minecraft.world.entity.Entity entity,
            TeleportTransition transition
    ) {
        if (entity instanceof net.minecraft.server.level.ServerPlayer) {
            return transition;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityPortalExitEvent.class)) {
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
        var to = new Location(toWorld, absolute.position().x, absolute.position().y, absolute.position().z, absolute.yRot(), absolute.xRot());
        var event = new EntityPortalExitEvent(fandEntity, from, to, to);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityPortalExitEvent listener failed", failure);
            return transition;
        }
        if (event.cancelled()) {
            return null;
        }
        if (sameLocation(to, event.after())) {
            return transition;
        }
        ServerLevel level = resolveLevel(event.after().world(), transition.newLevel());
        if (level == null) {
            LOGGER.warn("EntityPortalExitEvent targeted an unloaded world: {}", event.after().world().key().asString());
            return transition;
        }
        return new TeleportTransition(
                level,
                new Vec3(event.after().x(), event.after().y(), event.after().z()),
                transition.deltaMovement(),
                event.after().yaw(),
                event.after().pitch(),
                transition.missingRespawnBlock(),
                transition.asPassenger(),
                java.util.Set.of(),
                transition.postTeleportTransition());
    }

    public static boolean fireCreatePortal(net.minecraft.world.entity.Entity entity, ServerLevel level, List<BlockPos> positions) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityCreatePortalEvent.class)) {
            return true;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        var world = FandHooks.wrapWorld(level);
        if (fandEntity == null || world == null) {
            return true;
        }
        var event = new EntityCreatePortalEvent(
                fandEntity,
                positions.stream().map(pos -> new FandBlock(world, pos.getX(), pos.getY(), pos.getZ())).toList());
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityCreatePortalEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireItemDespawn(net.minecraft.world.entity.item.ItemEntity entity, int age) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(ItemDespawnEvent.class)) {
            return true;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        if (fandEntity == null) {
            return true;
        }
        var event = new ItemDespawnEvent(fandEntity, FandItemStacks.fromVanilla(entity.getItem()), age);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("ItemDespawnEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireItemMerge(
            net.minecraft.world.entity.item.ItemEntity target,
            net.minecraft.world.entity.item.ItemEntity source
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(ItemMergeEvent.class)) {
            return true;
        }
        var fandTarget = FandHooks.wrapEntity(target);
        var fandSource = FandHooks.wrapEntity(source);
        if (fandTarget == null || fandSource == null) {
            return true;
        }
        var event = new ItemMergeEvent(
                fandTarget,
                fandSource,
                FandItemStacks.fromVanilla(target.getItem()),
                FandItemStacks.fromVanilla(source.getItem()));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("ItemMergeEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static PickupItemResult fireEntityPickupItem(
            net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.world.entity.item.ItemEntity itemEntity,
            net.minecraft.world.item.ItemStack itemStack
    ) {
        var bus = FandHooks.events();
        if (entity instanceof ServerPlayer || !bus.hasListeners(EntityPickupItemEvent.class)) {
            return new PickupItemResult(true, itemStack);
        }
        var fandEntity = FandHooks.wrapLivingEntity(entity);
        if (fandEntity == null) {
            return new PickupItemResult(true, itemStack);
        }
        var event = new EntityPickupItemEvent(fandEntity, FandItemStacks.fromVanilla(itemStack));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityPickupItemEvent listener failed", failure);
            return new PickupItemResult(true, itemStack);
        }
        if (event.cancelled() || event.item().isEmpty()) {
            return new PickupItemResult(false, itemStack);
        }
        try {
            itemEntity.setItem(FandItemStacks.toVanilla(event.item()));
            return new PickupItemResult(true, itemEntity.getItem());
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityPickupItemEvent supplied an invalid item stack", failure);
            return new PickupItemResult(true, itemStack);
        }
    }

    public static net.minecraft.world.item.@Nullable ItemStack fireDropItem(
            net.minecraft.world.entity.Entity entity,
            ServerLevel level,
            Vec3 position,
            net.minecraft.world.item.ItemStack itemStack
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityDropItemEvent.class)) {
            return itemStack;
        }
        var world = FandHooks.wrapWorld(level);
        var fandEntity = FandHooks.wrapEntity(entity);
        if (world == null || fandEntity == null) {
            return itemStack;
        }
        var event = new EntityDropItemEvent(
                fandEntity,
                new Location(world, position.x, position.y, position.z, entity.getYRot(), entity.getXRot()),
                FandItemStacks.fromVanilla(itemStack));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityDropItemEvent listener failed", failure);
            return itemStack;
        }
        if (event.cancelled() || event.item().isEmpty()) {
            return null;
        }
        try {
            return FandItemStacks.toVanilla(event.item());
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityDropItemEvent supplied an invalid item stack", failure);
            return itemStack;
        }
    }

    public static boolean fireChangeBlock(
            net.minecraft.world.entity.Entity entity,
            ServerLevel level,
            BlockPos pos,
            BlockState oldState,
            BlockState newState,
            EntityChangeBlockEvent.Cause cause
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityChangeBlockEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        var fandEntity = FandHooks.wrapEntity(entity);
        if (world == null || fandEntity == null) {
            return true;
        }
        var event = new EntityChangeBlockEvent(
                fandEntity,
                new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()),
                FandBlockType.of(oldState.getBlock()),
                FandBlockType.of(newState.getBlock()),
                cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityChangeBlockEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireHangingPlace(
            net.minecraft.world.entity.player.@Nullable Player player,
            net.minecraft.world.entity.decoration.HangingEntity entity,
            BlockPos blockPos,
            Direction direction,
            net.minecraft.world.item.ItemStack itemStack
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(HangingPlaceEvent.class)) {
            return true;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        var fandEntity = FandHooks.wrapEntity(entity);
        if (world == null || fandEntity == null) {
            return true;
        }
        var event = new HangingPlaceEvent(
                player instanceof ServerPlayer serverPlayer
                        ? Optional.ofNullable(FandHooks.findPlayer(serverPlayer.getUUID()))
                        : Optional.empty(),
                fandEntity,
                new FandBlock(world, blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                face(direction),
                FandItemStacks.fromVanilla(itemStack));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("HangingPlaceEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireHangingBreak(
            net.minecraft.world.entity.decoration.BlockAttachedEntity entity,
            net.minecraft.world.entity.@Nullable Entity remover,
            HangingBreakEvent.Cause cause
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(HangingBreakEvent.class)) {
            return true;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        if (fandEntity == null) {
            return true;
        }
        var event = new HangingBreakEvent(
                fandEntity,
                Optional.ofNullable(remover).map(FandHooks::wrapEntity),
                cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("HangingBreakEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static @Nullable ItemFrameChangeResult fireItemFrameChange(
            net.minecraft.world.entity.decoration.ItemFrame itemFrame,
            ServerPlayer player,
            PlayerItemFrameChangeEvent.Action action,
            net.minecraft.world.item.ItemStack itemStack,
            int rotation
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerItemFrameChangeEvent.class)) {
            return new ItemFrameChangeResult(itemStack, rotation);
        }
        var fandPlayer = FandHooks.findPlayer(player.getUUID());
        var fandFrame = FandHooks.wrapEntity(itemFrame);
        if (fandPlayer == null || fandFrame == null) {
            return new ItemFrameChangeResult(itemStack, rotation);
        }
        var event = new PlayerItemFrameChangeEvent(
                fandPlayer,
                fandFrame,
                action,
                FandItemStacks.fromVanilla(itemStack),
                rotation);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerItemFrameChangeEvent listener failed", failure);
            return new ItemFrameChangeResult(itemStack, rotation);
        }
        if (event.cancelled()) {
            return null;
        }
        try {
            return new ItemFrameChangeResult(FandItemStacks.toVanilla(event.item()), event.rotation());
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerItemFrameChangeEvent supplied an invalid item stack", failure);
            return new ItemFrameChangeResult(itemStack, rotation);
        }
    }

    public static boolean fireVehicleDestroy(
            net.minecraft.world.entity.vehicle.VehicleEntity vehicle,
            net.minecraft.world.entity.@Nullable Entity attacker
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(VehicleDestroyEvent.class)) {
            return true;
        }
        var fandVehicle = FandHooks.wrapEntity(vehicle);
        if (fandVehicle == null) {
            return true;
        }
        var event = new VehicleDestroyEvent(fandVehicle, Optional.ofNullable(attacker).map(FandHooks::wrapEntity));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("VehicleDestroyEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static void fireVehicleMove(
            net.minecraft.world.entity.Entity vehicle,
            Vec3 fromPosition,
            Vec3 toPosition
    ) {
        var bus = FandHooks.events();
        if (!isVehicle(vehicle) || !bus.hasListeners(VehicleMoveEvent.class)) {
            return;
        }
        if (!(vehicle.level() instanceof ServerLevel level)) {
            return;
        }
        var fandVehicle = FandHooks.wrapEntity(vehicle);
        var world = FandHooks.wrapWorld(level);
        if (fandVehicle == null || world == null) {
            return;
        }
        var from = new Location(world, fromPosition.x, fromPosition.y, fromPosition.z, vehicle.getYRot(), vehicle.getXRot());
        var to = new Location(world, toPosition.x, toPosition.y, toPosition.z, vehicle.getYRot(), vehicle.getXRot());
        try {
            bus.fire(new VehicleMoveEvent(fandVehicle, from, to));
        } catch (RuntimeException failure) {
            LOGGER.warn("VehicleMoveEvent listener failed", failure);
        }
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

    public static @Nullable List<BlockPos> fireExplodeBlocks(
            ServerLevel level,
            net.minecraft.world.entity.@Nullable Entity source,
            Vec3 center,
            List<BlockPos> affectedPositions
    ) {
        var bus = FandHooks.events();
        boolean hasEntityListeners = bus.hasListeners(EntityExplodeEvent.class);
        if (source == null) {
            return BlockEvents.fireBlockExplode(level, BlockPos.containing(center), affectedPositions);
        }
        if (!hasEntityListeners) {
            return affectedPositions;
        }
        var world = FandHooks.wrapWorld(level);
        var fandSource = FandHooks.wrapEntity(source);
        if (world == null || fandSource == null) {
            return affectedPositions;
        }
        var affected = affectedPositions.stream()
                .map(pos -> new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()))
                .map(io.fand.api.block.Block.class::cast)
                .toList();
        var event = new EntityExplodeEvent(
                fandSource,
                new Location(world, center.x, center.y, center.z, 0.0F, 0.0F),
                affected);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityExplodeEvent listener failed", failure);
            return affectedPositions;
        }
        if (event.cancelled()) {
            return null;
        }
        return positionsInWorld(world, event.affectedBlocks());
    }

    public static boolean fireProjectileLaunch(Projectile projectile, net.minecraft.world.item.ItemStack itemStack) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(ProjectileLaunchEvent.class)) {
            return true;
        }
        var fandProjectile = FandHooks.wrapEntity(projectile);
        if (fandProjectile == null) {
            return true;
        }
        var event = new ProjectileLaunchEvent(
                fandProjectile,
                Optional.ofNullable(projectile.getOwner()).map(FandHooks::wrapEntity),
                FandItemStacks.fromVanilla(itemStack));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("ProjectileLaunchEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireShootBow(
            net.minecraft.world.entity.LivingEntity shooter,
            net.minecraft.world.item.ItemStack bow,
            net.minecraft.world.item.ItemStack consumable,
            Projectile projectile,
            float force
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityShootBowEvent.class)) {
            return true;
        }
        var fandShooter = FandHooks.wrapLivingEntity(shooter);
        var fandProjectile = FandHooks.wrapEntity(projectile);
        if (fandShooter == null || fandProjectile == null) {
            return true;
        }
        var event = new EntityShootBowEvent(
                fandShooter,
                FandItemStacks.fromVanilla(bow),
                FandItemStacks.fromVanilla(consumable),
                fandProjectile,
                force);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityShootBowEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireResurrect(
            net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.world.item.ItemStack itemStack,
            InteractionHand hand
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityResurrectEvent.class)) {
            return true;
        }
        var fandEntity = FandHooks.wrapLivingEntity(entity);
        if (fandEntity == null) {
            return true;
        }
        var event = new EntityResurrectEvent(
                fandEntity,
                FandItemStacks.fromVanilla(itemStack),
                hand == InteractionHand.OFF_HAND ? EntityResurrectEvent.Hand.OFF_HAND : EntityResurrectEvent.Hand.MAIN_HAND);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityResurrectEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireTransform(
            net.minecraft.world.entity.Mob entity,
            EntityType<?> targetType,
            ConversionParams params
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EntityTransformEvent.class)) {
            return true;
        }
        var fandEntity = FandHooks.wrapEntity(entity);
        if (fandEntity == null) {
            return true;
        }
        var identifier = BuiltInRegistries.ENTITY_TYPE.getKey(targetType);
        var event = new EntityTransformEvent(
                fandEntity,
                identifier == null
                        ? net.kyori.adventure.key.Key.key("minecraft:unknown")
                        : net.kyori.adventure.key.Key.key(identifier.getNamespace(), identifier.getPath()),
                params.type().name().equals("SPLIT_ON_DEATH")
                        ? EntityTransformEvent.Cause.SPLIT
                        : EntityTransformEvent.Cause.CONVERSION);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EntityTransformEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static @Nullable Map<net.minecraft.world.entity.LivingEntity, Double> firePotionSplash(
            ThrownSplashPotion potion,
            ServerLevel level,
            net.minecraft.world.item.ItemStack potionItem,
            Vec3 location,
            Map<net.minecraft.world.entity.LivingEntity, Double> affectedEntities
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PotionSplashEvent.class)) {
            return affectedEntities;
        }
        var world = FandHooks.wrapWorld(level);
        var fandPotion = FandHooks.wrapEntity(potion);
        if (world == null || fandPotion == null) {
            return affectedEntities;
        }
        var affected = new LinkedHashMap<io.fand.api.entity.LivingEntity, Double>();
        for (var entry : affectedEntities.entrySet()) {
            var wrapped = FandHooks.wrapLivingEntity(entry.getKey());
            if (wrapped != null) {
                affected.put(wrapped, entry.getValue());
            }
        }
        var event = new PotionSplashEvent(
                fandPotion,
                FandItemStacks.fromVanilla(potionItem),
                new Location(world, location.x, location.y, location.z, potion.getYRot(), potion.getXRot()),
                Optional.ofNullable(potion.getOwner()).map(FandHooks::wrapEntity),
                affected);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PotionSplashEvent listener failed", failure);
            return affectedEntities;
        }
        if (event.cancelled()) {
            return null;
        }
        var result = new LinkedHashMap<net.minecraft.world.entity.LivingEntity, Double>();
        for (var entry : event.affectedEntities().entrySet()) {
            var living = toVanillaLiving(entry.getKey());
            if (living != null && living.level() == potion.level()) {
                result.put(living, Math.max(0.0, Math.min(1.0, entry.getValue())));
            }
        }
        return result;
    }

    public static boolean fireLingeringPotionSplash(
            ThrownLingeringPotion potion,
            ServerLevel level,
            net.minecraft.world.item.ItemStack potionItem,
            Vec3 location
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(LingeringPotionSplashEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        var fandPotion = FandHooks.wrapEntity(potion);
        if (world == null || fandPotion == null) {
            return true;
        }
        var event = new LingeringPotionSplashEvent(
                fandPotion,
                FandItemStacks.fromVanilla(potionItem),
                new Location(world, location.x, location.y, location.z, potion.getYRot(), potion.getXRot()),
                Optional.ofNullable(potion.getOwner()).map(FandHooks::wrapEntity));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("LingeringPotionSplashEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static void fireProjectileHit(Projectile projectile, HitResult hitResult) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(ProjectileHitEvent.class)) {
            return;
        }
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        var fandProjectile = FandHooks.wrapEntity(projectile);
        var world = FandHooks.wrapWorld(level);
        if (fandProjectile == null || world == null) {
            return;
        }
        var hitLocation = new Location(
                world,
                hitResult.getLocation().x,
                hitResult.getLocation().y,
                hitResult.getLocation().z,
                0.0F,
                0.0F);
        Optional<io.fand.api.entity.Entity> hitEntity = Optional.empty();
        Optional<io.fand.api.block.Block> hitBlock = Optional.empty();
        ProjectileHitEvent.HitType hitType = switch (hitResult.getType()) {
            case ENTITY -> ProjectileHitEvent.HitType.ENTITY;
            case BLOCK -> ProjectileHitEvent.HitType.BLOCK;
            case MISS -> ProjectileHitEvent.HitType.MISS;
        };
        if (hitResult instanceof EntityHitResult entityHit) {
            hitEntity = Optional.ofNullable(FandHooks.wrapEntity(entityHit.getEntity()));
        } else if (hitResult instanceof BlockHitResult blockHit && !blockHit.isWorldBorderHit()) {
            BlockPos pos = blockHit.getBlockPos();
            hitBlock = Optional.of(new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()));
        }
        var event = new ProjectileHitEvent(fandProjectile, hitEntity, hitBlock, hitLocation, hitType);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("ProjectileHitEvent listener failed", failure);
        }
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

    private static EntityTeleportEvent.Cause currentTeleportCause(
            net.minecraft.world.level.Level from,
            ServerLevel to
    ) {
        EntityTeleportEvent.Cause explicit = NEXT_TELEPORT_CAUSE.get();
        return explicit == null ? teleportCause(from, to) : explicit;
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

    private static List<BlockPos> positionsInWorld(FandWorld world, List<io.fand.api.block.Block> blocks) {
        var positions = new LinkedHashSet<BlockPos>();
        for (var block : blocks) {
            if (!block.world().equals(world)) {
                continue;
            }
            positions.add(new BlockPos(block.x(), block.y(), block.z()));
        }
        return new ArrayList<>(positions);
    }

    private static boolean isVehicle(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.vehicle.VehicleEntity;
    }

    private static BlockFace face(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case EAST -> BlockFace.EAST;
            case WEST -> BlockFace.WEST;
            case UP -> BlockFace.UP;
            case DOWN -> BlockFace.DOWN;
        };
    }

    public record ExplosionResult(float radius, boolean fire) {
    }

    public record ItemFrameChangeResult(
            net.minecraft.world.item.ItemStack itemStack,
            int rotation
    ) {
    }

    public record PickupItemResult(
            boolean allowed,
            net.minecraft.world.item.ItemStack itemStack
    ) {
    }

    public record TargetResult(
            boolean allowed,
            net.minecraft.world.entity.@Nullable LivingEntity target
    ) {
    }

    private static EntityPotionEffectEvent.Effect effect(MobEffectInstance effect) {
        return new EntityPotionEffectEvent.Effect(
                effect.getDuration(),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon());
    }

    private static net.minecraft.world.entity.@Nullable LivingEntity toVanillaLiving(io.fand.api.entity.LivingEntity entity) {
        if (entity instanceof FandPlayer player) {
            return player.handle();
        }
        if (entity instanceof FandLivingEntity living) {
            return living.handle();
        }
        return null;
    }
}
