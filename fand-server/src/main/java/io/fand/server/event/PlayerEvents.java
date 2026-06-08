package io.fand.server.event;

import io.fand.api.event.player.PlayerDropItemEvent;
import io.fand.api.event.player.PlayerAdvancementDoneEvent;
import io.fand.api.event.player.PlayerArmorStandManipulateEvent;
import io.fand.api.event.player.PlayerBedEnterEvent;
import io.fand.api.event.player.PlayerBedLeaveEvent;
import io.fand.api.event.player.PlayerBucketEmptyEvent;
import io.fand.api.event.player.PlayerBucketFillEvent;
import io.fand.api.event.player.PlayerChangedMainHandEvent;
import io.fand.api.event.player.PlayerChangedWorldEvent;
import io.fand.api.event.player.PlayerClientBrandEvent;
import io.fand.api.event.player.PlayerEditBookEvent;
import io.fand.api.event.player.PlayerEggThrowEvent;
import io.fand.api.event.player.PlayerExperienceChangeEvent;
import io.fand.api.event.player.PlayerFoodLevelChangeEvent;
import io.fand.api.event.player.PlayerGameModeChangeEvent;
import io.fand.api.event.player.PlayerInteractEntityEvent;
import io.fand.api.event.player.PlayerInteractEvent;
import io.fand.api.event.player.PlayerLeashEntityEvent;
import io.fand.api.event.player.PlayerFishEvent;
import io.fand.api.event.player.PlayerItemConsumeEvent;
import io.fand.api.event.player.PlayerItemDamageEvent;
import io.fand.api.event.player.PlayerItemHeldEvent;
import io.fand.api.event.player.PlayerKickEvent;
import io.fand.api.event.player.PlayerLevelChangeEvent;
import io.fand.api.event.player.PlayerLocaleChangeEvent;
import io.fand.api.event.player.PlayerPickupItemEvent;
import io.fand.api.event.player.PlayerPortalEvent;
import io.fand.api.event.player.PlayerRecipeDiscoverEvent;
import io.fand.api.event.player.PlayerRespawnEvent;
import io.fand.api.event.player.PlayerResourcePackStatusEvent;
import io.fand.api.event.player.PlayerShearEntityEvent;
import io.fand.api.event.player.PlayerStatisticIncrementEvent;
import io.fand.api.event.player.PlayerSwapHandItemsEvent;
import io.fand.api.event.player.PlayerTeleportEvent;
import io.fand.api.event.player.PlayerToggleSneakEvent;
import io.fand.api.event.player.PlayerToggleSprintEvent;
import io.fand.api.event.player.PlayerUnleashEntityEvent;
import io.fand.api.event.player.PlayerVelocityEvent;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.server.block.FandBlock;
import io.fand.server.command.AdventureBridge;
import io.fand.server.entity.GameModes;
import io.fand.server.command.CommandEvents;
import io.fand.server.entity.FandPlayer;
import io.fand.server.hooks.FandHooks;
import io.fand.server.item.FandItemStacks;
import io.fand.server.recipe.FandRecipes;
import io.fand.server.world.FandWorld;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.stats.Stat;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlayerEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerEvents.class);
    private static final ThreadLocal<PlayerTeleportEvent.Cause> NEXT_TELEPORT_CAUSE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> SUPPRESS_TELEPORT_EVENT = ThreadLocal.withInitial(() -> 0);

    private PlayerEvents() {
    }

    public static <T> T withTeleportCause(PlayerTeleportEvent.Cause cause, java.util.concurrent.Callable<T> task) {
        PlayerTeleportEvent.Cause previous = NEXT_TELEPORT_CAUSE.get();
        NEXT_TELEPORT_CAUSE.set(Objects.requireNonNull(cause, "cause"));
        try {
            return task.call();
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new RuntimeException(failure);
        } finally {
            restoreTeleportCause(previous);
        }
    }

    public static void withTeleportCause(PlayerTeleportEvent.Cause cause, Runnable task) {
        PlayerTeleportEvent.Cause previous = NEXT_TELEPORT_CAUSE.get();
        NEXT_TELEPORT_CAUSE.set(Objects.requireNonNull(cause, "cause"));
        try {
            task.run();
        } finally {
            restoreTeleportCause(previous);
        }
    }

    public static <T> T withoutTeleportEvent(java.util.concurrent.Callable<T> task) throws Exception {
        int previous = SUPPRESS_TELEPORT_EVENT.get();
        SUPPRESS_TELEPORT_EVENT.set(previous + 1);
        try {
            return task.call();
        } finally {
            restoreSuppressDepth(previous);
        }
    }

    public static net.minecraft.world.item.ItemStack fireDrop(
            ServerPlayer player,
            net.minecraft.world.item.ItemStack itemStack,
            boolean randomly,
            boolean thrownFromHand) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerDropItemEvent.class)) {
            return itemStack;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return itemStack;
        }
        var event = new PlayerDropItemEvent(
                fandPlayer,
                FandItemStacks.fromVanilla(itemStack),
                randomly,
                thrownFromHand);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerDropItemEvent listener failed", failure);
            return itemStack;
        }
        if (event.cancelled() || event.item().isEmpty()) {
            if (thrownFromHand || randomly) {
                player.getInventory().placeItemBackInInventory(itemStack.copy());
            }
            return null;
        }
        try {
            return FandItemStacks.toVanilla(event.item());
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerDropItemEvent supplied an invalid item stack", failure);
            return itemStack;
        }
    }

    public static boolean firePickup(ServerPlayer player, ItemEntity entity) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerPickupItemEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        var original = entity.getItem();
        var event = new PlayerPickupItemEvent(fandPlayer, FandItemStacks.fromVanilla(original));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerPickupItemEvent listener failed", failure);
            return true;
        }
        if (event.cancelled() || event.item().isEmpty()) {
            return false;
        }
        try {
            var replacement = FandItemStacks.toVanilla(event.item());
            if (!net.minecraft.world.item.ItemStack.matches(original, replacement)) {
                entity.setItem(replacement);
            }
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerPickupItemEvent supplied an invalid item stack", failure);
        }
        return true;
    }

    public static boolean fireItemConsume(ServerPlayer player, net.minecraft.world.item.ItemStack itemStack) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerItemConsumeEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        var event = new PlayerItemConsumeEvent(fandPlayer, FandItemStacks.fromVanilla(itemStack));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerItemConsumeEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static int fireItemDamage(
            ServerPlayer player,
            net.minecraft.world.item.ItemStack itemStack,
            int damage
    ) {
        if (damage <= 0) {
            return damage;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerItemDamageEvent.class)) {
            return damage;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return damage;
        }
        var event = new PlayerItemDamageEvent(fandPlayer, FandItemStacks.fromVanilla(itemStack), damage);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerItemDamageEvent listener failed", failure);
            return damage;
        }
        return event.cancelled() ? 0 : event.damage();
    }

    public static @Nullable SwapHandItems fireSwapHandItems(
            ServerPlayer player,
            net.minecraft.world.item.ItemStack mainHandItem,
            net.minecraft.world.item.ItemStack offHandItem
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerSwapHandItemsEvent.class)) {
            return new SwapHandItems(mainHandItem, offHandItem);
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return new SwapHandItems(mainHandItem, offHandItem);
        }
        var event = new PlayerSwapHandItemsEvent(
                fandPlayer,
                FandItemStacks.fromVanilla(mainHandItem),
                FandItemStacks.fromVanilla(offHandItem));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerSwapHandItemsEvent listener failed", failure);
            return new SwapHandItems(mainHandItem, offHandItem);
        }
        if (event.cancelled()) {
            return null;
        }
        try {
            return new SwapHandItems(
                    FandItemStacks.toVanilla(event.mainHandItem()),
                    FandItemStacks.toVanilla(event.offHandItem()));
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerSwapHandItemsEvent supplied an invalid item stack", failure);
            return new SwapHandItems(mainHandItem, offHandItem);
        }
    }

    public static boolean fireToggleSneak(ServerPlayer player, boolean sneaking) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerToggleSneakEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        var event = new PlayerToggleSneakEvent(fandPlayer, sneaking);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerToggleSneakEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireToggleSprint(ServerPlayer player, boolean sprinting) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerToggleSprintEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        var event = new PlayerToggleSprintEvent(fandPlayer, sprinting);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerToggleSprintEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireItemHeld(ServerPlayer player, int previousSlot, int newSlot) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerItemHeldEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        var inventory = player.getInventory();
        var event = new PlayerItemHeldEvent(
                fandPlayer,
                previousSlot,
                newSlot,
                FandItemStacks.fromVanilla(inventory.getItem(previousSlot)),
                FandItemStacks.fromVanilla(inventory.getItem(newSlot)));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerItemHeldEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireInteractEntity(
            ServerPlayer player,
            net.minecraft.world.entity.Entity target,
            InteractionHand hand,
            net.minecraft.world.item.ItemStack itemStack,
            boolean preciseInteraction
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerInteractEntityEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var fandTarget = FandHooks.wrapEntity(target);
        if (fandPlayer == null || fandTarget == null) {
            return true;
        }
        var event = new PlayerInteractEntityEvent(
                fandPlayer,
                fandTarget,
                hand(hand),
                FandItemStacks.fromVanilla(itemStack),
                preciseInteraction);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerInteractEntityEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireShearEntity(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.entity.Entity target,
            InteractionHand hand,
            net.minecraft.world.item.ItemStack tool
    ) {
        var bus = FandHooks.events();
        if (!(player instanceof ServerPlayer serverPlayer) || !bus.hasListeners(PlayerShearEntityEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(serverPlayer.getUUID());
        var fandTarget = FandHooks.wrapEntity(target);
        if (fandPlayer == null || fandTarget == null) {
            return true;
        }
        var event = new PlayerShearEntityEvent(
                fandPlayer,
                fandTarget,
                hand(hand),
                FandItemStacks.fromVanilla(tool));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerShearEntityEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireLeashEntity(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.Entity holder,
            PlayerLeashEntityEvent.Cause cause
    ) {
        var bus = FandHooks.events();
        if (!(player instanceof ServerPlayer serverPlayer) || !bus.hasListeners(PlayerLeashEntityEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(serverPlayer.getUUID());
        var fandEntity = FandHooks.wrapEntity(entity);
        var fandHolder = FandHooks.wrapEntity(holder);
        if (fandPlayer == null || fandEntity == null) {
            return true;
        }
        var event = new PlayerLeashEntityEvent(
                fandPlayer,
                fandEntity,
                fandHolder,
                cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerLeashEntityEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireUnleashEntity(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.@Nullable Entity holder,
            boolean dropLead
    ) {
        var bus = FandHooks.events();
        if (!(player instanceof ServerPlayer serverPlayer) || !bus.hasListeners(PlayerUnleashEntityEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(serverPlayer.getUUID());
        var fandEntity = FandHooks.wrapEntity(entity);
        if (fandPlayer == null || fandEntity == null) {
            return true;
        }
        var event = new PlayerUnleashEntityEvent(
                fandPlayer,
                fandEntity,
                holder == null ? null : FandHooks.wrapEntity(holder),
                dropLead);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerUnleashEntityEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static @Nullable GameType fireGameModeChange(ServerPlayer player, GameType from, GameType to) {
        if (from == to) {
            return to;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerGameModeChangeEvent.class)) {
            return to;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return to;
        }
        var event = new PlayerGameModeChangeEvent(fandPlayer, GameModes.toApi(from), GameModes.toApi(to));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerGameModeChangeEvent listener failed", failure);
            return to;
        }
        return event.cancelled() ? null : GameModes.toVanilla(event.toGameMode());
    }

    public static net.minecraft.network.chat.@Nullable Component fireKick(
            ServerPlayer player,
            net.minecraft.network.chat.Component reason
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerKickEvent.class)) {
            return reason;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return reason;
        }
        net.kyori.adventure.text.Component adventureReason;
        try {
            adventureReason = AdventureBridge.fromVanilla(reason, player.registryAccess());
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerKickEvent reason conversion failed; using plain text fallback", failure);
            adventureReason = net.kyori.adventure.text.Component.text(reason.getString());
        }
        var event = new PlayerKickEvent(fandPlayer, adventureReason);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerKickEvent listener failed", failure);
            return reason;
        }
        if (event.cancelled()) {
            return null;
        }
        return AdventureBridge.toVanillaOrFallback(event.reason(), reason, player.registryAccess());
    }

    public static @Nullable FoodChange fireFoodLevelChange(
            ServerPlayer player,
            int fromLevel,
            int toLevel,
            float fromSaturation,
            float toSaturation,
            PlayerFoodLevelChangeEvent.Cause cause
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerFoodLevelChangeEvent.class)) {
            return new FoodChange(toLevel, toSaturation);
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return new FoodChange(toLevel, toSaturation);
        }
        var event = new PlayerFoodLevelChangeEvent(
                fandPlayer,
                fromLevel,
                toLevel,
                fromSaturation,
                toSaturation,
                cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerFoodLevelChangeEvent listener failed", failure);
            return new FoodChange(toLevel, toSaturation);
        }
        return event.cancelled() ? null : new FoodChange(event.toLevel(), event.toSaturation());
    }

    public static net.minecraft.world.item.@Nullable ItemStack fireBucketFill(
            ServerPlayer player,
            BlockPos pos,
            net.minecraft.world.item.ItemStack bucket,
            net.minecraft.world.item.ItemStack result
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerBucketFillEvent.class)) {
            return result;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var world = FandHooks.wrapWorld(player.level());
        if (fandPlayer == null || world == null) {
            return result;
        }
        var event = new PlayerBucketFillEvent(
                fandPlayer,
                new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()),
                FandItemStacks.fromVanilla(bucket),
                FandItemStacks.fromVanilla(result));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerBucketFillEvent listener failed", failure);
            return result;
        }
        if (event.cancelled() || event.resultItem().isEmpty()) {
            return null;
        }
        try {
            return FandItemStacks.toVanilla(event.resultItem());
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerBucketFillEvent supplied an invalid result item", failure);
            return result;
        }
    }

    public static net.minecraft.world.item.@Nullable ItemStack fireBucketEmpty(
            ServerPlayer player,
            BlockPos pos,
            Fluid fluid,
            net.minecraft.world.item.ItemStack bucket,
            net.minecraft.world.item.ItemStack result
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerBucketEmptyEvent.class)) {
            return result;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var world = FandHooks.wrapWorld(player.level());
        if (fandPlayer == null || world == null) {
            return result;
        }
        var identifier = BuiltInRegistries.FLUID.getKey(fluid);
        var event = new PlayerBucketEmptyEvent(
                fandPlayer,
                new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()),
                identifier == null ? Key.key("minecraft:empty") : Key.key(identifier.getNamespace(), identifier.getPath()),
                FandItemStacks.fromVanilla(bucket),
                FandItemStacks.fromVanilla(result));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerBucketEmptyEvent listener failed", failure);
            return result;
        }
        if (event.cancelled() || event.resultItem().isEmpty()) {
            return null;
        }
        try {
            return FandItemStacks.toVanilla(event.resultItem());
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerBucketEmptyEvent supplied an invalid result item", failure);
            return result;
        }
    }

    public static int fireExperienceChange(ServerPlayer player, int amount) {
        if (amount == 0) {
            return amount;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerExperienceChangeEvent.class)) {
            return amount;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return amount;
        }
        var event = new PlayerExperienceChangeEvent(fandPlayer, amount);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerExperienceChangeEvent listener failed", failure);
            return amount;
        }
        return event.cancelled() ? 0 : event.amount();
    }

    public static boolean fireBedEnter(ServerPlayer player, BlockPos pos) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerBedEnterEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var world = FandHooks.wrapWorld(player.level());
        if (fandPlayer == null || world == null) {
            return true;
        }
        var event = new PlayerBedEnterEvent(fandPlayer, new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerBedEnterEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static void fireBedLeave(ServerPlayer player, BlockPos pos, boolean forcefulWakeUp) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerBedLeaveEvent.class)) {
            return;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var world = FandHooks.wrapWorld(player.level());
        if (fandPlayer == null || world == null) {
            return;
        }
        var event = new PlayerBedLeaveEvent(
                fandPlayer,
                new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()),
                forcefulWakeUp);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerBedLeaveEvent listener failed", failure);
        }
    }

    public static void fireAdvancementDone(ServerPlayer player, Identifier advancement) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerAdvancementDoneEvent.class)) {
            return;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return;
        }
        var key = Key.key(advancement.getNamespace(), advancement.getPath());
        try {
            bus.fire(new PlayerAdvancementDoneEvent(fandPlayer, key));
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerAdvancementDoneEvent listener failed", failure);
        }
    }

    public static @Nullable TeleportTransition fireTeleport(ServerPlayer player, TeleportTransition transition) {
        var bus = FandHooks.events();
        if (SUPPRESS_TELEPORT_EVENT.get() > 0 || !bus.hasListeners(PlayerTeleportEvent.class)) {
            return transition;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return transition;
        }
        var fromWorld = FandHooks.wrapWorld(player.level());
        var toWorld = FandHooks.wrapWorld(transition.newLevel());
        if (fromWorld == null || toWorld == null) {
            return transition;
        }
        PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(
                PositionMoveRotation.of(player),
                PositionMoveRotation.of(transition),
                transition.relatives());
        var from = new Location(fromWorld, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        var to = new Location(
                toWorld,
                absolute.position().x,
                absolute.position().y,
                absolute.position().z,
                absolute.yRot(),
                absolute.xRot());
        var event = new PlayerTeleportEvent(fandPlayer, from, to, currentTeleportCause());
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerTeleportEvent listener failed", failure);
            return transition;
        }
        if (event.cancelled()) {
            return null;
        }
        if (sameLocation(to, event.to())) {
            return transition;
        }
        ServerLevel level = resolveLevel(event.to().world(), player);
        if (level == null) {
            LOGGER.warn("PlayerTeleportEvent targeted an unloaded world: {}", event.to().world().key().asString());
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

    public static @Nullable TeleportTransition firePortal(ServerPlayer player, TeleportTransition transition) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerPortalEvent.class)) {
            return transition;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var fromWorld = FandHooks.wrapWorld(player.level());
        var toWorld = FandHooks.wrapWorld(transition.newLevel());
        if (fandPlayer == null || fromWorld == null || toWorld == null) {
            return transition;
        }
        PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(
                PositionMoveRotation.of(player),
                PositionMoveRotation.of(transition),
                transition.relatives());
        var from = new Location(fromWorld, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        var to = new Location(
                toWorld,
                absolute.position().x,
                absolute.position().y,
                absolute.position().z,
                absolute.yRot(),
                absolute.xRot());
        var event = new PlayerPortalEvent(fandPlayer, from, to);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerPortalEvent listener failed", failure);
            return transition;
        }
        if (event.cancelled()) {
            return null;
        }
        if (sameLocation(to, event.to())) {
            return transition;
        }
        ServerLevel level = resolveLevel(event.to().world(), player);
        if (level == null) {
            LOGGER.warn("PlayerPortalEvent targeted an unloaded world: {}", event.to().world().key().asString());
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

    public static void fireChangedWorld(ServerPlayer player, ServerLevel oldLevel, ServerLevel newLevel) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerChangedWorldEvent.class)) {
            return;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var fromWorld = FandHooks.wrapWorld(oldLevel);
        var toWorld = FandHooks.wrapWorld(newLevel);
        if (fandPlayer == null || fromWorld == null || toWorld == null) {
            return;
        }
        try {
            bus.fire(new PlayerChangedWorldEvent(fandPlayer, fromWorld, toWorld));
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerChangedWorldEvent listener failed", failure);
        }
    }

    public static void fireResourcePackStatus(
            ServerPlayer player,
            UUID id,
            PlayerResourcePackStatusEvent.Status status
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerResourcePackStatusEvent.class)) {
            return;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return;
        }
        try {
            bus.fire(new PlayerResourcePackStatusEvent(fandPlayer, id, status));
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerResourcePackStatusEvent listener failed", failure);
        }
    }

    public static void fireLocaleChange(ServerPlayer player, String oldLocale, String newLocale) {
        if (oldLocale.equalsIgnoreCase(newLocale)) {
            return;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerLocaleChangeEvent.class)) {
            return;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return;
        }
        try {
            bus.fire(new PlayerLocaleChangeEvent(fandPlayer, oldLocale, newLocale));
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerLocaleChangeEvent listener failed", failure);
        }
    }

    public static void fireChangedMainHand(ServerPlayer player, HumanoidArm oldMainHand, HumanoidArm newMainHand) {
        if (oldMainHand == newMainHand) {
            return;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerChangedMainHandEvent.class)) {
            return;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return;
        }
        try {
            bus.fire(new PlayerChangedMainHandEvent(fandPlayer, mainHand(oldMainHand), mainHand(newMainHand)));
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerChangedMainHandEvent listener failed", failure);
        }
    }

    public static void fireClientBrand(ServerPlayer player, String brand) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerClientBrandEvent.class)) {
            return;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return;
        }
        try {
            bus.fire(new PlayerClientBrandEvent(fandPlayer, brand));
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerClientBrandEvent listener failed", failure);
        }
    }

    public static void fireLevelChange(ServerPlayer player, int oldLevel, int newLevel) {
        if (oldLevel == newLevel) {
            return;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerLevelChangeEvent.class)) {
            return;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return;
        }
        try {
            bus.fire(new PlayerLevelChangeEvent(fandPlayer, oldLevel, newLevel));
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerLevelChangeEvent listener failed", failure);
        }
    }

    public static @Nullable Vec3 fireVelocity(ServerPlayer player, Vec3 velocity) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerVelocityEvent.class)) {
            return velocity;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return velocity;
        }
        var event = new PlayerVelocityEvent(fandPlayer, velocity.x, velocity.y, velocity.z);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerVelocityEvent listener failed", failure);
            return velocity;
        }
        return event.cancelled() ? null : new Vec3(event.x(), event.y(), event.z());
    }

    public static @Nullable Integer fireStatisticIncrement(ServerPlayer player, Stat<?> stat, int previousValue, int newValue) {
        if (newValue <= previousValue) {
            return newValue;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerStatisticIncrementEvent.class)) {
            return newValue;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return newValue;
        }
        var event = new PlayerStatisticIncrementEvent(fandPlayer, statisticKey(stat), previousValue, newValue);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerStatisticIncrementEvent listener failed", failure);
            return newValue;
        }
        return event.cancelled() ? null : event.newValue();
    }

    public static boolean fireRecipeDiscover(ServerPlayer player, Collection<RecipeHolder<?>> recipes) {
        if (recipes.isEmpty()) {
            return true;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerRecipeDiscoverEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        var event = new PlayerRecipeDiscoverEvent(
                fandPlayer,
                recipes.stream().map(FandRecipes::fromVanilla).toList());
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerRecipeDiscoverEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static net.minecraft.world.item.@Nullable ItemStack fireEditBook(
            ServerPlayer player,
            int slot,
            net.minecraft.world.item.ItemStack previousBook,
            net.minecraft.world.item.ItemStack newBook,
            Optional<String> title,
            boolean signing
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerEditBookEvent.class)) {
            return newBook;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return newBook;
        }
        var event = new PlayerEditBookEvent(
                fandPlayer,
                slot,
                FandItemStacks.fromVanilla(previousBook),
                FandItemStacks.fromVanilla(newBook),
                title.orElse(null),
                signing);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerEditBookEvent listener failed", failure);
            return newBook;
        }
        if (event.cancelled() || event.newBook().isEmpty()) {
            return null;
        }
        try {
            return FandItemStacks.toVanilla(event.newBook());
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerEditBookEvent supplied an invalid book item", failure);
            return newBook;
        }
    }

    public static EggThrowResult fireEggThrow(
            ServerPlayer player,
            net.minecraft.world.entity.Entity egg,
            boolean hatching,
            int hatchCount
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerEggThrowEvent.class)) {
            return new EggThrowResult(hatching, hatchCount);
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var fandEgg = FandHooks.wrapEntity(egg);
        if (fandPlayer == null || fandEgg == null) {
            return new EggThrowResult(hatching, hatchCount);
        }
        var event = new PlayerEggThrowEvent(fandPlayer, fandEgg, hatching, hatchCount);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerEggThrowEvent listener failed", failure);
            return new EggThrowResult(hatching, hatchCount);
        }
        return new EggThrowResult(event.hatching(), event.hatchCount());
    }

    public static boolean fireArmorStandManipulate(
            ServerPlayer player,
            ArmorStand armorStand,
            EquipmentSlot slot,
            net.minecraft.world.item.ItemStack playerItem,
            net.minecraft.world.item.ItemStack armorStandItem
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerArmorStandManipulateEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var fandStand = FandHooks.wrapEntity(armorStand);
        if (fandPlayer == null || fandStand == null) {
            return true;
        }
        var event = new PlayerArmorStandManipulateEvent(
                fandPlayer,
                fandStand,
                equipmentSlot(slot),
                FandItemStacks.fromVanilla(playerItem),
                FandItemStacks.fromVanilla(armorStandItem));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerArmorStandManipulateEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireFish(
            ServerPlayer player,
            net.minecraft.world.entity.Entity hook,
            PlayerFishEvent.State state,
            Optional<net.minecraft.world.entity.Entity> caught,
            List<net.minecraft.world.item.ItemStack> drops
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerFishEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var fandHook = FandHooks.wrapEntity(hook);
        if (fandPlayer == null || fandHook == null) {
            return true;
        }
        var event = new PlayerFishEvent(
                fandPlayer,
                fandHook,
                state,
                caught.map(FandHooks::wrapEntity).orElse(null),
                drops.stream().map(FandItemStacks::fromVanilla).toList());
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerFishEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static ServerLevel fireRespawn(
            ServerPlayer player,
            ServerLevel currentLevel,
            boolean keepAllPlayerData,
            net.minecraft.world.entity.Entity.RemovalReason removalReason) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerRespawnEvent.class)) {
            return currentLevel;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var world = FandHooks.wrapWorld(currentLevel);
        if (fandPlayer == null || world == null) {
            return currentLevel;
        }
        var location = new Location(world, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        var event = new PlayerRespawnEvent(fandPlayer, location, respawnCause(removalReason), keepAllPlayerData);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerRespawnEvent listener failed", failure);
            return currentLevel;
        }
        if (sameLocation(location, event.respawnLocation())) {
            return currentLevel;
        }
        ServerLevel level = resolveLevel(event.respawnLocation().world(), player);
        if (level == null) {
            LOGGER.warn("PlayerRespawnEvent targeted an unloaded world: {}", event.respawnLocation().world().key().asString());
            return currentLevel;
        }
        if (level != player.level()) {
            player.setServerLevel(level);
        }
        player.snapTo(
                event.respawnLocation().x(),
                event.respawnLocation().y(),
                event.respawnLocation().z(),
                event.respawnLocation().yaw(),
                event.respawnLocation().pitch());
        return level;
    }

    private static PlayerTeleportEvent.Cause currentTeleportCause() {
        PlayerTeleportEvent.Cause explicit = NEXT_TELEPORT_CAUSE.get();
        if (explicit != null) {
            return explicit;
        }
        return CommandEvents.inCommandContext() ? PlayerTeleportEvent.Cause.COMMAND : PlayerTeleportEvent.Cause.UNKNOWN;
    }

    private static void restoreTeleportCause(PlayerTeleportEvent.Cause previous) {
        if (previous == null) {
            NEXT_TELEPORT_CAUSE.remove();
        } else {
            NEXT_TELEPORT_CAUSE.set(previous);
        }
    }

    private static void restoreSuppressDepth(int previous) {
        if (previous == 0) {
            SUPPRESS_TELEPORT_EVENT.remove();
        } else {
            SUPPRESS_TELEPORT_EVENT.set(previous);
        }
    }

    private static PlayerRespawnEvent.Cause respawnCause(net.minecraft.world.entity.Entity.RemovalReason reason) {
        return switch (reason) {
            case KILLED -> PlayerRespawnEvent.Cause.DEATH;
            case CHANGED_DIMENSION -> PlayerRespawnEvent.Cause.DIMENSION_CHANGE;
            default -> PlayerRespawnEvent.Cause.UNKNOWN;
        };
    }

    private static @Nullable ServerLevel resolveLevel(World world, ServerPlayer player) {
        if (world instanceof FandWorld fandWorld) {
            return fandWorld.handle();
        }
        var key = world.key();
        var server = player.level().getServer();
        if (server == null) {
            return null;
        }
        for (var level : server.getAllLevels()) {
            var identifier = level.dimension().identifier();
            if (identifier.getNamespace().equals(key.namespace()) && identifier.getPath().equals(key.value())) {
                return level;
            }
        }
        return null;
    }

    private static PlayerInteractEvent.Hand hand(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND
                ? PlayerInteractEvent.Hand.OFF_HAND
                : PlayerInteractEvent.Hand.MAIN_HAND;
    }

    private static PlayerChangedMainHandEvent.MainHand mainHand(HumanoidArm arm) {
        return arm == HumanoidArm.LEFT
                ? PlayerChangedMainHandEvent.MainHand.LEFT
                : PlayerChangedMainHandEvent.MainHand.RIGHT;
    }

    private static PlayerArmorStandManipulateEvent.EquipmentSlot equipmentSlot(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> PlayerArmorStandManipulateEvent.EquipmentSlot.MAIN_HAND;
            case OFFHAND -> PlayerArmorStandManipulateEvent.EquipmentSlot.OFF_HAND;
            case FEET -> PlayerArmorStandManipulateEvent.EquipmentSlot.FEET;
            case LEGS -> PlayerArmorStandManipulateEvent.EquipmentSlot.LEGS;
            case CHEST -> PlayerArmorStandManipulateEvent.EquipmentSlot.CHEST;
            case HEAD -> PlayerArmorStandManipulateEvent.EquipmentSlot.HEAD;
            case BODY -> PlayerArmorStandManipulateEvent.EquipmentSlot.BODY;
            case SADDLE -> PlayerArmorStandManipulateEvent.EquipmentSlot.BODY;
        };
    }

    private static Key statisticKey(Stat<?> stat) {
        var typeKey = BuiltInRegistries.STAT_TYPE.getKey(stat.getType());
        var valueKey = statisticValueKey(stat);
        String namespace = valueKey == null ? "minecraft" : valueKey.getNamespace();
        String type = typeKey == null ? "unknown" : typeKey.getPath();
        String value = valueKey == null ? stat.getName().replace(':', '.') : valueKey.getPath();
        return Key.key(namespace, type + "/" + value);
    }

    private static <T> net.minecraft.resources.Identifier statisticValueKey(Stat<T> stat) {
        return stat.getType().getRegistry().getKey(stat.getValue());
    }

    private static boolean sameLocation(Location a, Location b) {
        return a.world().equals(b.world())
                && Double.compare(a.x(), b.x()) == 0
                && Double.compare(a.y(), b.y()) == 0
                && Double.compare(a.z(), b.z()) == 0
                && Float.compare(a.yaw(), b.yaw()) == 0
                && Float.compare(a.pitch(), b.pitch()) == 0;
    }

    public record SwapHandItems(
            net.minecraft.world.item.ItemStack mainHandItem,
            net.minecraft.world.item.ItemStack offHandItem
    ) {
    }

    public record FoodChange(int level, float saturation) {
    }

    public record EggThrowResult(boolean hatching, int hatchCount) {
    }
}
