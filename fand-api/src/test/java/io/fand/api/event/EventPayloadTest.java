package io.fand.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.Entity;
import io.fand.api.entity.GameMode;
import io.fand.api.entity.Player;
import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.event.block.BlockBurnEvent;
import io.fand.api.event.block.BlockCanBuildEvent;
import io.fand.api.event.block.BlockChangeEvent;
import io.fand.api.event.block.BlockDispenseEvent;
import io.fand.api.event.block.BlockExplodeEvent;
import io.fand.api.event.block.BlockFadeEvent;
import io.fand.api.event.block.BlockFertilizeEvent;
import io.fand.api.event.block.BlockFormEvent;
import io.fand.api.event.block.BlockGrowEvent;
import io.fand.api.event.block.BlockIgniteEvent;
import io.fand.api.event.block.BlockPhysicsEvent;
import io.fand.api.event.block.BlockFace;
import io.fand.api.event.block.BlockFromToEvent;
import io.fand.api.event.block.BlockMultiPlaceEvent;
import io.fand.api.event.block.BlockPistonExtendEvent;
import io.fand.api.event.block.BlockPistonPushEvent;
import io.fand.api.event.block.BlockPistonRetractEvent;
import io.fand.api.event.block.BlockPlaceEvent;
import io.fand.api.event.block.BlockRedstoneEvent;
import io.fand.api.event.block.BlockSpreadEvent;
import io.fand.api.event.block.CauldronLevelChangeEvent;
import io.fand.api.event.block.FluidFlowEvent;
import io.fand.api.event.block.LeavesDecayEvent;
import io.fand.api.event.block.PortalCreateEvent;
import io.fand.api.event.block.SignChangeEvent;
import io.fand.api.event.block.SpongeAbsorbEvent;
import io.fand.api.event.command.CommandExecuteEvent;
import io.fand.api.event.command.TabCompleteEvent;
import io.fand.api.event.entity.EntityBreedEvent;
import io.fand.api.event.entity.EntityChangeBlockEvent;
import io.fand.api.event.entity.EntityCombustByBlockEvent;
import io.fand.api.event.entity.EntityCombustByEntityEvent;
import io.fand.api.event.entity.EntityCombustEvent;
import io.fand.api.event.entity.EntityDamageByEntityEvent;
import io.fand.api.event.entity.EntityDamageByBlockEvent;
import io.fand.api.event.entity.EntityDamageEvent;
import io.fand.api.event.entity.EntityDeathEvent;
import io.fand.api.event.entity.EntityDismountEvent;
import io.fand.api.event.entity.EntityDropItemEvent;
import io.fand.api.event.entity.EntityExplodeEvent;
import io.fand.api.event.entity.EntityCreatePortalEvent;
import io.fand.api.event.entity.EntityMountEvent;
import io.fand.api.event.entity.EntityPickupItemEvent;
import io.fand.api.event.entity.EntityPortalEnterEvent;
import io.fand.api.event.entity.EntityPortalEvent;
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
import io.fand.api.event.inventory.BrewEvent;
import io.fand.api.event.inventory.BrewingStandFuelEvent;
import io.fand.api.event.inventory.BlockCookEvent;
import io.fand.api.event.inventory.CraftItemEvent;
import io.fand.api.event.inventory.EnchantItemEvent;
import io.fand.api.event.inventory.EnchantmentOffer;
import io.fand.api.event.inventory.FurnaceBurnEvent;
import io.fand.api.event.inventory.FurnaceExtractEvent;
import io.fand.api.event.inventory.FurnaceStartSmeltEvent;
import io.fand.api.event.inventory.FurnaceSmeltEvent;
import io.fand.api.event.inventory.HopperMoveItemEvent;
import io.fand.api.event.inventory.HopperPickupItemEvent;
import io.fand.api.event.inventory.ClickType;
import io.fand.api.event.inventory.DragType;
import io.fand.api.event.inventory.InventoryCreativeEvent;
import io.fand.api.event.inventory.InventoryTradeEvent;
import io.fand.api.event.inventory.InventoryAction;
import io.fand.api.event.inventory.InventoryClickEvent;
import io.fand.api.event.inventory.InventoryDragEvent;
import io.fand.api.event.inventory.InventoryMoveItemEvent;
import io.fand.api.event.inventory.InventoryPickupItemEvent;
import io.fand.api.event.inventory.PrepareAnvilEvent;
import io.fand.api.event.inventory.PrepareItemEnchantEvent;
import io.fand.api.event.inventory.PrepareItemCraftEvent;
import io.fand.api.event.inventory.PrepareTradeEvent;
import io.fand.api.event.inventory.PrepareSmithingEvent;
import io.fand.api.recipe.Recipe;
import io.fand.api.recipe.RecipeType;
import io.fand.api.event.player.AsyncPlayerPreLoginEvent;
import io.fand.api.event.player.PlayerAdvancementDoneEvent;
import io.fand.api.event.player.PlayerArmorStandManipulateEvent;
import io.fand.api.event.player.PlayerBedEnterEvent;
import io.fand.api.event.player.PlayerBedLeaveEvent;
import io.fand.api.event.player.PlayerBucketEmptyEvent;
import io.fand.api.event.player.PlayerBucketFillEvent;
import io.fand.api.event.player.PlayerChangedMainHandEvent;
import io.fand.api.event.player.PlayerChangedWorldEvent;
import io.fand.api.event.player.PlayerCommandPreprocessEvent;
import io.fand.api.event.player.PlayerClientBrandEvent;
import io.fand.api.event.player.PlayerDeathEvent;
import io.fand.api.event.player.PlayerDropItemEvent;
import io.fand.api.event.player.PlayerEditBookEvent;
import io.fand.api.event.player.PlayerEggThrowEvent;
import io.fand.api.event.player.PlayerExperienceChangeEvent;
import io.fand.api.event.player.PlayerFoodLevelChangeEvent;
import io.fand.api.event.player.PlayerGameModeChangeEvent;
import io.fand.api.event.player.PlayerFishEvent;
import io.fand.api.event.player.PlayerInteractEntityEvent;
import io.fand.api.event.player.PlayerInteractEvent;
import io.fand.api.event.player.PlayerItemConsumeEvent;
import io.fand.api.event.player.PlayerItemDamageEvent;
import io.fand.api.event.player.PlayerItemHeldEvent;
import io.fand.api.event.player.PlayerKickEvent;
import io.fand.api.event.player.PlayerLeashEntityEvent;
import io.fand.api.event.player.PlayerLevelChangeEvent;
import io.fand.api.event.player.PlayerLocaleChangeEvent;
import io.fand.api.event.player.PlayerLoginEvent;
import io.fand.api.event.player.PlayerMoveEvent;
import io.fand.api.event.player.PlayerPickupItemEvent;
import io.fand.api.event.player.PlayerPortalEvent;
import io.fand.api.event.player.PlayerPreLoginEvent;
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
import io.fand.api.event.permission.PermissionCheckEvent;
import io.fand.api.event.server.ServerListPingEvent;
import io.fand.api.event.vehicle.VehicleCreateEvent;
import io.fand.api.event.vehicle.VehicleDestroyEvent;
import io.fand.api.event.vehicle.VehicleEnterEvent;
import io.fand.api.event.vehicle.VehicleExitEvent;
import io.fand.api.event.vehicle.VehicleMoveEvent;
import io.fand.api.event.world.ChunkLoadEvent;
import io.fand.api.event.world.ChunkUnloadEvent;
import io.fand.api.event.world.SpawnChangeEvent;
import io.fand.api.event.world.StructureGrowEvent;
import io.fand.api.event.world.ThunderChangeEvent;
import io.fand.api.event.world.TimeSkipEvent;
import io.fand.api.event.world.WeatherChangeEvent;
import io.fand.api.event.world.WorldLoadEvent;
import io.fand.api.event.world.WorldSaveEvent;
import io.fand.api.event.world.WorldUnloadEvent;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.world.Difficulty;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.api.world.WorldBorder;
import java.net.InetSocketAddress;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class EventPayloadTest {

    @Test
    void playerInteractEventCarriesHandItem() {
        var player = proxy(Player.class);
        var item = new ItemStack(new TestItemType(Key.key("minecraft:compass"), 64), 1);

        var event = new PlayerInteractEvent(
                player,
                PlayerInteractEvent.Action.RIGHT_CLICK_AIR,
                PlayerInteractEvent.Hand.MAIN_HAND,
                null,
                item);

        assertThat(event.player()).isSameAs(player);
        assertThat(event.item()).isSameAs(item);
        assertThat(event.block()).isEmpty();
    }

    @Test
    void playerInteractEventKeepsLegacyConstructorEmptyItem() {
        var event = new PlayerInteractEvent(
                proxy(Player.class),
                PlayerInteractEvent.Action.RIGHT_CLICK_AIR,
                PlayerInteractEvent.Hand.MAIN_HAND,
                null);

        assertThat(event.item()).isEqualTo(ItemStack.EMPTY);
    }

    @Test
    void inventoryClickEventCarriesSlotContext() {
        var inventory = proxy(Inventory.class);
        var current = new ItemStack(new TestItemType(Key.key("minecraft:stone"), 64), 3);
        var cursor = new ItemStack(new TestItemType(Key.key("minecraft:diamond"), 64), 1);

        var event = new InventoryClickEvent(
                proxy(Player.class),
                inventory,
                14,
                14,
                -1,
                5,
                ClickType.SWAP,
                2,
                current,
                cursor);

        assertThat(event.inventory()).isSameAs(inventory);
        assertThat(event.slot()).isEqualTo(14);
        assertThat(event.rawSlot()).isEqualTo(14);
        assertThat(event.containerSlot()).isEqualTo(-1);
        assertThat(event.playerInventorySlot()).isEqualTo(5);
        assertThat(event.playerInventoryClick()).isTrue();
        assertThat(event.containerClick()).isFalse();
        assertThat(event.outsideClick()).isFalse();
        assertThat(event.hotbarButton()).isEqualTo(2);
        assertThat(event.action()).isEqualTo(InventoryAction.UNKNOWN);
    }

    @Test
    void inventoryClickEventCarriesAction() {
        var event = new InventoryClickEvent(
                proxy(Player.class),
                proxy(Inventory.class),
                14,
                14,
                -1,
                5,
                ClickType.PICKUP,
                InventoryAction.PLACE_ALL,
                0,
                ItemStack.EMPTY,
                stack("minecraft:stone", 4));

        assertThat(event.action()).isEqualTo(InventoryAction.PLACE_ALL);
    }

    @Test
    void inventoryMoveItemEventCarriesMutableItemAndDirection() {
        var source = proxy(Inventory.class);
        var destination = proxy(Inventory.class);
        var stone = stack("minecraft:stone", 1);
        var diamond = stack("minecraft:diamond", 1);

        var event = new InventoryMoveItemEvent(source, destination, stone, true);
        event.setItem(diamond);
        event.setCancelled(true);
        var pickup = new InventoryPickupItemEvent(destination, stone);
        pickup.setItem(diamond);
        pickup.setCancelled(true);

        assertThat(event.source()).isSameAs(source);
        assertThat(event.destination()).isSameAs(destination);
        assertThat(event.item()).isSameAs(diamond);
        assertThat(event.sourceInitiated()).isTrue();
        assertThat(event.cancelled()).isTrue();
        assertThat(pickup.inventory()).isSameAs(destination);
        assertThat(pickup.item()).isSameAs(diamond);
        assertThat(pickup.cancelled()).isTrue();
    }

    @Test
    void inventoryDragEventCarriesSlotsAndCancellation() {
        var player = proxy(Player.class);
        var inventory = proxy(Inventory.class);
        var cursor = stack("minecraft:stone", 32);

        var event = new InventoryDragEvent(player, inventory, DragType.EVEN, Set.of(10, 11, 12), cursor);
        event.setCancelled(true);

        assertThat(event.player()).isSameAs(player);
        assertThat(event.inventory()).isSameAs(inventory);
        assertThat(event.dragType()).isEqualTo(DragType.EVEN);
        assertThat(event.slots()).containsExactlyInAnyOrder(10, 11, 12);
        assertThat(event.cursorItem()).isSameAs(cursor);
        assertThat(event.cancelled()).isTrue();
    }

    @Test
    void inventoryClickEventKeepsLegacyConstructorDefaults() {
        var event = new InventoryClickEvent(
                proxy(Player.class),
                proxy(Inventory.class),
                InventoryClickEvent.OUTSIDE,
                ClickType.DROP,
                0,
                ItemStack.EMPTY,
                ItemStack.EMPTY);

        assertThat(event.rawSlot()).isEqualTo(InventoryClickEvent.OUTSIDE);
        assertThat(event.containerSlot()).isEqualTo(-1);
        assertThat(event.playerInventorySlot()).isEqualTo(-1);
        assertThat(event.outsideClick()).isTrue();
        assertThat(event.hotbarButton()).isEqualTo(-1);
    }

    @Test
    void entityDamageEventCarriesSourceEntities() {
        var victim = proxy(LivingEntity.class);
        var direct = proxy(LivingEntity.class);
        var attacker = proxy(LivingEntity.class);

        var event = new EntityDamageEvent(victim, "minecraft:player_attack", 4.0, direct, attacker);

        assertThat(event.entity()).isSameAs(victim);
        assertThat(event.directEntity()).contains(direct);
        assertThat(event.attacker()).contains(attacker);
        assertThat(event.amount()).isEqualTo(4.0);
    }

    @Test
    void entityDamageByEntityEventSpecializesDamageEvent() {
        var victim = proxy(LivingEntity.class);
        var direct = proxy(LivingEntity.class);
        var attacker = proxy(LivingEntity.class);

        var event = new EntityDamageByEntityEvent(
                victim,
                "minecraft:player_attack",
                6.0,
                attacker,
                direct);
        event.setAmount(2.5);
        event.setCancelled(true);

        assertThat(event).isInstanceOf(EntityDamageEvent.class);
        assertThat(event.entity()).isSameAs(victim);
        assertThat(event.damager()).isSameAs(attacker);
        assertThat(event.attacker()).contains(attacker);
        assertThat(event.directEntity()).contains(direct);
        assertThat(event.amount()).isEqualTo(2.5);
        assertThat(event.cancelled()).isTrue();
    }

    @Test
    void entityDamageEventKeepsLegacyConstructorEmptySources() {
        var event = new EntityDamageEvent(proxy(LivingEntity.class), "minecraft:fall", 2.0);

        assertThat(event.directEntity()).isEmpty();
        assertThat(event.attacker()).isEmpty();
    }

    @Test
    void commandExecuteEventNormalisesAndMutatesCommand() {
        var sender = proxy(io.fand.api.command.CommandSender.class);
        var event = new CommandExecuteEvent(sender, " /say hello");

        assertThat(event.sender()).isSameAs(sender);
        assertThat(event.originalCommand()).isEqualTo("say hello");
        event.setCommand("/fanddemo");
        event.setCancelled(true);

        assertThat(event.command()).isEqualTo("fanddemo");
        assertThat(event.cancelled()).isTrue();
    }

    @Test
    void playerCommandPreprocessEventNormalisesAndMutatesCommand() {
        var player = proxy(Player.class);
        var event = new PlayerCommandPreprocessEvent(player, "/fwhere");

        assertThat(event.player()).isSameAs(player);
        assertThat(event.originalCommand()).isEqualTo("fwhere");
        event.setCommand("fanddemo");

        assertThat(event.command()).isEqualTo("fanddemo");
    }

    @Test
    void playerDropAndPickupEventsCarryMutableItems() {
        var player = proxy(Player.class);
        var stone = stack("minecraft:stone", 3);
        var diamond = stack("minecraft:diamond", 1);

        var drop = new PlayerDropItemEvent(player, stone, true, false);
        drop.setItem(diamond);
        drop.setCancelled(true);
        var pickup = new PlayerPickupItemEvent(player, stone);
        pickup.setItem(diamond);

        assertThat(drop.player()).isSameAs(player);
        assertThat(drop.randomMotion()).isTrue();
        assertThat(drop.thrownFromHand()).isFalse();
        assertThat(drop.item()).isSameAs(diamond);
        assertThat(drop.cancelled()).isTrue();
        assertThat(pickup.item()).isSameAs(diamond);
    }

    @Test
    void playerTeleportEventCarriesMutableDestinationAndCause() {
        var player = proxy(Player.class);
        var from = location("minecraft:overworld", 0, 64, 0);
        var to = location("minecraft:overworld", 10, 70, 10);
        var retargeted = location("minecraft:the_nether", 2, 80, 2);

        var event = new PlayerTeleportEvent(player, from, to, PlayerTeleportEvent.Cause.COMMAND);
        event.setTo(retargeted);
        event.setCancelled(true);

        assertThat(event.from()).isSameAs(from);
        assertThat(event.to()).isSameAs(retargeted);
        assertThat(event.cause()).isEqualTo(PlayerTeleportEvent.Cause.COMMAND);
        assertThat(event.cancelled()).isTrue();
    }

    @Test
    void playerMoveEventCarriesLocationsAndCancellation() {
        var player = proxy(Player.class);
        var from = location("minecraft:overworld", 0, 64, 0);
        var to = location("minecraft:overworld", 1, 64, 0);

        var event = new PlayerMoveEvent(player, from, to);
        event.setCancelled(true);

        assertThat(event.player()).isSameAs(player);
        assertThat(event.from()).isSameAs(from);
        assertThat(event.to()).isSameAs(to);
        assertThat(event.cancelled()).isTrue();
    }

    @Test
    void playerRespawnEventCarriesMutableLocation() {
        var player = proxy(Player.class);
        var original = location("minecraft:overworld", 0, 64, 0);
        var retargeted = location("minecraft:overworld", 5, 80, 5);

        var event = new PlayerRespawnEvent(player, original, PlayerRespawnEvent.Cause.DEATH, false);
        event.setRespawnLocation(retargeted);

        assertThat(event.player()).isSameAs(player);
        assertThat(event.respawnLocation()).isSameAs(retargeted);
        assertThat(event.cause()).isEqualTo(PlayerRespawnEvent.Cause.DEATH);
        assertThat(event.keepAllPlayerData()).isFalse();
    }

    @Test
    void entityDeathEventCarriesFatalSourceEntities() {
        var victim = proxy(LivingEntity.class);
        var attacker = proxy(LivingEntity.class);

        var event = new EntityDeathEvent(victim, "minecraft:mob_attack", null, attacker);

        assertThat(event.entity()).isSameAs(victim);
        assertThat(event.cause()).isEqualTo("minecraft:mob_attack");
        assertThat(event.directEntity()).isEmpty();
        assertThat(event.attacker()).contains(attacker);
    }

    @Test
    void playerDeathEventCarriesMutableDeathMessage() {
        var player = proxy(Player.class);
        var replacement = Component.text("custom death");

        var event = new PlayerDeathEvent(player, Component.text("old death"));
        event.setDeathMessage(replacement);

        assertThat(event.player()).isSameAs(player);
        assertThat(event.deathMessage()).isEqualTo(replacement);
    }

    @Test
    void entityLifecycleEventsCarryTypedCausesAndMutableTeleport() {
        var entity = proxy(Entity.class);
        var from = location("minecraft:overworld", 0, 64, 0);
        var to = location("minecraft:overworld", 10, 64, 0);
        var retargeted = location("minecraft:the_end", 0, 80, 0);

        var spawn = new EntitySpawnEvent(entity, EntitySpawnEvent.Cause.SPAWNER);
        spawn.setCancelled(true);
        var remove = new EntityRemoveEvent(entity, EntityRemoveEvent.Cause.UNLOADED_TO_CHUNK);
        var teleport = new EntityTeleportEvent(entity, from, to, EntityTeleportEvent.Cause.DIMENSION_CHANGE);
        teleport.setTo(retargeted);

        assertThat(spawn.entity()).isSameAs(entity);
        assertThat(spawn.cause()).isEqualTo(EntitySpawnEvent.Cause.SPAWNER);
        assertThat(spawn.cancelled()).isTrue();
        assertThat(remove.cause()).isEqualTo(EntityRemoveEvent.Cause.UNLOADED_TO_CHUNK);
        assertThat(teleport.to()).isSameAs(retargeted);
    }

    @Test
    void explosionPrimeEventCarriesSourceAndMutableShape() {
        var entity = proxy(Entity.class);
        var location = location("minecraft:overworld", 1, 2, 3);

        var event = new ExplosionPrimeEvent(location, entity, 4.0F, true);
        event.setRadius(-1.0F);
        event.setFire(false);
        event.setCancelled(true);

        assertThat(event.location()).isSameAs(location);
        assertThat(event.source()).contains(entity);
        assertThat(event.radius()).isZero();
        assertThat(event.fire()).isFalse();
        assertThat(event.cancelled()).isTrue();
    }

    @Test
    void entityDetailEventsCarryTargetsAndMutableCombustion() {
        var projectile = proxy(Entity.class);
        var hitEntity = proxy(Entity.class);
        var hitBlock = proxy(Block.class);
        var location = location("minecraft:overworld", 1, 2, 3);
        var projectileHit = new ProjectileHitEvent(
                projectile,
                hitEntity,
                hitBlock,
                location,
                ProjectileHitEvent.HitType.ENTITY);
        var combust = new EntityCombustEvent(
                hitEntity,
                projectile,
                EntityCombustEvent.Cause.PROJECTILE,
                5.0F);
        combust.setDurationSeconds(-1.0F);
        combust.setCancelled(true);
        var regain = new EntityRegainHealthEvent(
                proxy(LivingEntity.class),
                -2.0,
                EntityRegainHealthEvent.Cause.MAGIC);
        regain.setAmount(3.5);
        regain.setCancelled(true);

        assertThat(projectileHit.projectile()).isSameAs(projectile);
        assertThat(projectileHit.hitEntity()).contains(hitEntity);
        assertThat(projectileHit.hitBlock()).contains(hitBlock);
        assertThat(projectileHit.hitLocation()).isSameAs(location);
        assertThat(projectileHit.hitType()).isEqualTo(ProjectileHitEvent.HitType.ENTITY);
        assertThat(combust.durationSeconds()).isZero();
        assertThat(combust.source()).contains(projectile);
        assertThat(combust.cancelled()).isTrue();
        assertThat(regain.amount()).isEqualTo(3.5);
        assertThat(regain.cause()).isEqualTo(EntityRegainHealthEvent.Cause.MAGIC);
        assertThat(regain.cancelled()).isTrue();
    }

    @Test
    void entityFineEventsCarryMutableTargetsAndCancellations() {
        var entity = proxy(LivingEntity.class);
        var oldTarget = proxy(LivingEntity.class);
        var newTarget = proxy(LivingEntity.class);
        var replacement = proxy(LivingEntity.class);
        var player = proxy(Player.class);
        var vehicle = proxy(Entity.class);
        var child = proxy(LivingEntity.class);

        var target = new EntityTargetEvent(
                entity,
                oldTarget,
                newTarget,
                EntityTargetEvent.Cause.CLOSEST_ENTITY);
        target.setTarget(replacement);
        target.setCancelled(true);
        var effect = new EntityPotionEffectEvent(
                entity,
                Key.key("minecraft:speed"),
                null,
                new EntityPotionEffectEvent.Effect(120, 1, false, true, true),
                vehicle,
                EntityPotionEffectEvent.Action.ADDED);
        effect.setCancelled(true);
        var mount = new EntityMountEvent(vehicle, entity);
        mount.setCancelled(true);
        var dismount = new EntityDismountEvent(vehicle, entity);
        var tame = new EntityTameEvent(entity, player);
        var breed = new EntityBreedEvent(entity, replacement, player, child);

        assertThat(target.oldTarget()).contains(oldTarget);
        assertThat(target.target()).contains(replacement);
        assertThat(target.cause()).isEqualTo(EntityTargetEvent.Cause.CLOSEST_ENTITY);
        assertThat(target.cancelled()).isTrue();
        assertThat(effect.effect()).isEqualTo(Key.key("minecraft:speed"));
        assertThat(effect.newEffect()).get().extracting(EntityPotionEffectEvent.Effect::durationTicks).isEqualTo(120);
        assertThat(effect.source()).contains(vehicle);
        assertThat(effect.cancelled()).isTrue();
        assertThat(mount.vehicle()).isSameAs(entity);
        assertThat(mount.cancelled()).isTrue();
        assertThat(dismount.entity()).isSameAs(vehicle);
        assertThat(tame.owner()).isSameAs(player);
        assertThat(breed.child()).isSameAs(child);
        assertThat(breed.breeder()).contains(player);
    }

    @Test
    void newlySpecializedEntityEventsCarryTypedSourcesAndPortalState() {
        var entity = proxy(Entity.class);
        var living = proxy(LivingEntity.class);
        var oldTarget = proxy(LivingEntity.class);
        var target = proxy(LivingEntity.class);
        var sourceEntity = proxy(Entity.class);
        var block = proxy(Block.class);
        var from = location("minecraft:overworld", 0, 64, 0);
        var to = location("minecraft:the_nether", 1, 80, 1);
        var after = location("minecraft:the_end", 2, 90, 2);

        var targetEvent = new EntityTargetLivingEntityEvent(
                living,
                oldTarget,
                target,
                EntityTargetEvent.Cause.TARGET_ATTACKED_ENTITY);
        var byBlockDamage = new EntityDamageByBlockEvent(living, "minecraft:cactus", 2.0, block);
        byBlockDamage.setAmount(3.5);
        byBlockDamage.setCancelled(true);
        var combustBlock = new EntityCombustByBlockEvent(entity, block, EntityCombustEvent.Cause.FIRE, -1.0F);
        combustBlock.setDurationSeconds(4.0F);
        var combustEntity = new EntityCombustByEntityEvent(entity, sourceEntity, EntityCombustEvent.Cause.ATTACK, 2.0F);
        combustEntity.setCancelled(true);
        var portalEnter = new EntityPortalEnterEvent(entity, from);
        var portalExit = new EntityPortalExitEvent(entity, from, to, to);
        portalExit.setAfter(after);
        portalExit.setCancelled(true);
        var createPortal = new EntityCreatePortalEvent(entity, List.of(block));
        createPortal.setCancelled(true);

        assertThat(targetEvent).isInstanceOf(EntityTargetEvent.class);
        assertThat(targetEvent.oldTarget()).contains(oldTarget);
        assertThat(targetEvent.target()).contains(target);
        assertThat(targetEvent.livingTarget()).isSameAs(target);
        assertThat(byBlockDamage).isInstanceOf(EntityDamageEvent.class);
        assertThat(byBlockDamage.damager()).isSameAs(block);
        assertThat(byBlockDamage.amount()).isEqualTo(3.5);
        assertThat(byBlockDamage.cancelled()).isTrue();
        assertThat(combustBlock.combuster()).isSameAs(block);
        assertThat(combustBlock.source()).isEmpty();
        assertThat(combustBlock.durationSeconds()).isEqualTo(4.0F);
        assertThat(combustEntity.combuster()).isSameAs(sourceEntity);
        assertThat(combustEntity.source()).contains(sourceEntity);
        assertThat(combustEntity.cancelled()).isTrue();
        assertThat(portalEnter.location()).isSameAs(from);
        assertThat(portalExit).isInstanceOf(EntityPortalEvent.class);
        assertThat(portalExit.after()).isSameAs(after);
        assertThat(portalExit.cancelled()).isTrue();
        assertThat(createPortal.blocks()).containsExactly(block);
        assertThat(createPortal.cancelled()).isTrue();
    }

    @Test
    void playerItemAndStateEventsCarryMutableFields() {
        var player = proxy(Player.class);
        var stone = stack("minecraft:stone", 1);
        var diamond = stack("minecraft:diamond", 1);

        var consume = new PlayerItemConsumeEvent(player, stone);
        consume.setCancelled(true);
        var damage = new PlayerItemDamageEvent(player, stone, 3);
        damage.setDamage(5);
        var swap = new PlayerSwapHandItemsEvent(player, stone, diamond);
        swap.setMainHandItem(diamond);
        swap.setOffHandItem(stone);
        var sneak = new PlayerToggleSneakEvent(player, true);
        var sprint = new PlayerToggleSprintEvent(player, false);

        assertThat(consume.cancelled()).isTrue();
        assertThat(damage.damage()).isEqualTo(5);
        assertThat(swap.mainHandItem()).isSameAs(diamond);
        assertThat(swap.offHandItem()).isSameAs(stone);
        assertThat(sneak.sneaking()).isTrue();
        assertThat(sprint.sprinting()).isFalse();
    }

    @Test
    void playerFineEventsCarryBucketsHeldSlotAndEntityInteraction() {
        var player = proxy(Player.class);
        var block = proxy(Block.class);
        var entity = proxy(Entity.class);
        var emptyBucket = stack("minecraft:bucket", 1);
        var waterBucket = stack("minecraft:water_bucket", 1);
        var lavaBucket = stack("minecraft:lava_bucket", 1);

        var held = new PlayerItemHeldEvent(player, 1, 2, emptyBucket, waterBucket);
        held.setCancelled(true);
        var interact = new PlayerInteractEntityEvent(
                player,
                entity,
                PlayerInteractEvent.Hand.MAIN_HAND,
                emptyBucket,
                true);
        interact.setCancelled(true);
        var fill = new PlayerBucketFillEvent(player, block, emptyBucket, waterBucket);
        fill.setResultItem(lavaBucket);
        fill.setCancelled(true);
        var empty = new PlayerBucketEmptyEvent(player, block, Key.key("minecraft:water"), waterBucket, emptyBucket);

        assertThat(held.previousSlot()).isEqualTo(1);
        assertThat(held.newSlot()).isEqualTo(2);
        assertThat(held.previousItem()).isSameAs(emptyBucket);
        assertThat(held.newItem()).isSameAs(waterBucket);
        assertThat(held.cancelled()).isTrue();
        assertThat(interact.entity()).isSameAs(entity);
        assertThat(interact.item()).isSameAs(emptyBucket);
        assertThat(interact.preciseInteraction()).isTrue();
        assertThat(interact.cancelled()).isTrue();
        assertThat(fill.resultItem()).isSameAs(lavaBucket);
        assertThat(fill.cancelled()).isTrue();
        assertThat(empty.fluid()).isEqualTo(Key.key("minecraft:water"));
    }

    @Test
    void playerDetailEventsCarryMutableFields() {
        var player = proxy(Player.class);
        var bed = proxy(Block.class);
        var food = new PlayerFoodLevelChangeEvent(
                player,
                18,
                20,
                1.0F,
                4.0F,
                PlayerFoodLevelChangeEvent.Cause.EAT);
        food.setToLevel(19);
        food.setToSaturation(50.0F);
        food.setCancelled(true);
        var xp = new PlayerExperienceChangeEvent(player, 5);
        xp.setAmount(12);
        var bedEnter = new PlayerBedEnterEvent(player, bed);
        bedEnter.setCancelled(true);
        var bedLeave = new PlayerBedLeaveEvent(player, bed, true);
        var advancement = new PlayerAdvancementDoneEvent(player, Key.key("minecraft:story/mine_stone"));

        assertThat(food.fromLevel()).isEqualTo(18);
        assertThat(food.toLevel()).isEqualTo(19);
        assertThat(food.toSaturation()).isEqualTo(50.0F);
        assertThat(food.cause()).isEqualTo(PlayerFoodLevelChangeEvent.Cause.EAT);
        assertThat(food.cancelled()).isTrue();
        assertThat(xp.amount()).isEqualTo(12);
        assertThat(bedEnter.bed()).isSameAs(bed);
        assertThat(bedEnter.cancelled()).isTrue();
        assertThat(bedLeave.forcefulWakeUp()).isTrue();
        assertThat(advancement.advancement()).isEqualTo(Key.key("minecraft:story/mine_stone"));
    }

    @Test
    void playerGameModeAndKickEventsAreMutableAndCancellable() {
        var player = proxy(Player.class);
        var kick = new PlayerKickEvent(player, net.kyori.adventure.text.Component.text("bye"));
        kick.setReason(net.kyori.adventure.text.Component.text("later"));
        kick.setCancelled(true);
        var gameMode = new PlayerGameModeChangeEvent(player, GameMode.SURVIVAL, GameMode.CREATIVE);
        gameMode.setToGameMode(GameMode.ADVENTURE);

        assertThat(kick.reason()).isEqualTo(net.kyori.adventure.text.Component.text("later"));
        assertThat(kick.cancelled()).isTrue();
        assertThat(gameMode.fromGameMode()).isEqualTo(GameMode.SURVIVAL);
        assertThat(gameMode.toGameMode()).isEqualTo(GameMode.ADVENTURE);
    }

    @Test
    void newPlayerLifecycleAndStateEventsCarryMutableFields() {
        var player = proxy(Player.class);
        var entity = proxy(Entity.class);
        var address = InetSocketAddress.createUnresolved("127.0.0.1", 25565);
        var id = java.util.UUID.randomUUID();
        var previousBook = stack("minecraft:writable_book", 1);
        var signedBook = stack("minecraft:written_book", 1);
        var recipe = new TestRecipe(Key.key("fand:test_recipe"), RecipeType.SHAPELESS, signedBook);

        var asyncPreLogin = new AsyncPlayerPreLoginEvent(
                id,
                "Steve",
                address,
                AsyncPlayerPreLoginEvent.Result.ALLOWED,
                Component.text("ok"));
        asyncPreLogin.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("no"));
        asyncPreLogin.allow();
        var preLogin = new PlayerPreLoginEvent(
                id,
                "Alex",
                address,
                PlayerPreLoginEvent.Result.ALLOWED,
                Component.text("ok"));
        preLogin.disallow(PlayerPreLoginEvent.Result.KICK_WHITELIST, Component.text("whitelist"));
        var armorStand = new PlayerArmorStandManipulateEvent(
                player,
                entity,
                PlayerArmorStandManipulateEvent.EquipmentSlot.HEAD,
                stack("minecraft:diamond_helmet", 1),
                ItemStack.EMPTY);
        armorStand.setCancelled(true);
        var editBook = new PlayerEditBookEvent(player, 3, previousBook, previousBook, "Demo", true);
        editBook.setNewBook(signedBook);
        editBook.setCancelled(true);
        var egg = new PlayerEggThrowEvent(player, entity, false, -3);
        egg.setHatching(true);
        egg.setHatchCount(5);
        var level = new PlayerLevelChangeEvent(player, 1, 7);
        var velocity = new PlayerVelocityEvent(player, 1.0, 2.0, 3.0);
        velocity.setVelocity(-1.0, 0.0, 0.5);
        velocity.setCancelled(true);
        var mainHand = new PlayerChangedMainHandEvent(
                player,
                PlayerChangedMainHandEvent.MainHand.LEFT,
                PlayerChangedMainHandEvent.MainHand.RIGHT);
        var statistic = new PlayerStatisticIncrementEvent(player, Key.key("minecraft:custom/jump"), 10, 11);
        statistic.setNewValue(-1);
        statistic.setCancelled(true);
        var recipeDiscover = new PlayerRecipeDiscoverEvent(player, List.of(recipe));
        recipeDiscover.setCancelled(true);

        assertThat(asyncPreLogin.uniqueId()).isEqualTo(id);
        assertThat(asyncPreLogin.address()).isSameAs(address);
        assertThat(asyncPreLogin.result()).isEqualTo(AsyncPlayerPreLoginEvent.Result.ALLOWED);
        assertThat(asyncPreLogin.kickMessage()).isEqualTo(Component.text("no"));
        assertThat(preLogin.name()).isEqualTo("Alex");
        assertThat(preLogin.result()).isEqualTo(PlayerPreLoginEvent.Result.KICK_WHITELIST);
        assertThat(preLogin.kickMessage()).isEqualTo(Component.text("whitelist"));
        assertThat(armorStand.slot()).isEqualTo(PlayerArmorStandManipulateEvent.EquipmentSlot.HEAD);
        assertThat(armorStand.playerItem().type().key()).isEqualTo(Key.key("minecraft:diamond_helmet"));
        assertThat(armorStand.cancelled()).isTrue();
        assertThat(editBook.slot()).isEqualTo(3);
        assertThat(editBook.previousBook()).isSameAs(previousBook);
        assertThat(editBook.newBook()).isSameAs(signedBook);
        assertThat(editBook.title()).contains("Demo");
        assertThat(editBook.signing()).isTrue();
        assertThat(editBook.cancelled()).isTrue();
        assertThat(egg.hatching()).isTrue();
        assertThat(egg.hatchCount()).isEqualTo(5);
        assertThat(level.oldLevel()).isEqualTo(1);
        assertThat(level.newLevel()).isEqualTo(7);
        assertThat(velocity.x()).isEqualTo(-1.0);
        assertThat(velocity.y()).isZero();
        assertThat(velocity.z()).isEqualTo(0.5);
        assertThat(velocity.cancelled()).isTrue();
        assertThat(mainHand.oldMainHand()).isEqualTo(PlayerChangedMainHandEvent.MainHand.LEFT);
        assertThat(mainHand.newMainHand()).isEqualTo(PlayerChangedMainHandEvent.MainHand.RIGHT);
        assertThat(statistic.statistic()).isEqualTo(Key.key("minecraft:custom/jump"));
        assertThat(statistic.previousValue()).isEqualTo(10);
        assertThat(statistic.newValue()).isZero();
        assertThat(statistic.cancelled()).isTrue();
        assertThat(recipeDiscover.recipes()).containsExactly(recipe);
        assertThat(recipeDiscover.cancelled()).isTrue();
    }

    @Test
    void blockLowLevelEventsCarryBlockTypes() {
        var block = proxy(Block.class);
        var oldType = proxy(BlockType.class);
        var newType = proxy(BlockType.class);

        var change = new BlockChangeEvent(block, oldType, newType, 3);
        change.setCancelled(true);
        var physics = new BlockPhysicsEvent(block, oldType);

        assertThat(change.block()).isSameAs(block);
        assertThat(change.oldType()).isSameAs(oldType);
        assertThat(change.newType()).isSameAs(newType);
        assertThat(change.updateFlags()).isEqualTo(3);
        assertThat(change.cancelled()).isTrue();
        assertThat(physics.sourceType()).isSameAs(oldType);
    }

    @Test
    void blockDetailEventsCarryCausesAndMutableCancellation() {
        var sourceBlock = proxy(Block.class);
        var block = proxy(Block.class);
        var oldType = proxy(BlockType.class);
        var newType = proxy(BlockType.class);

        var burn = new BlockBurnEvent(block, oldType, sourceBlock);
        burn.setCancelled(true);
        var fade = new BlockFadeEvent(block, oldType, newType, BlockFadeEvent.Cause.MELT);
        var grow = new BlockGrowEvent(block, oldType, newType, BlockGrowEvent.Cause.BONEMEAL);
        var ignite = new BlockIgniteEvent(block, newType, BlockIgniteEvent.Cause.SPREAD, sourceBlock);
        ignite.setCancelled(true);
        var spread = new BlockSpreadEvent(sourceBlock, block, oldType, newType, BlockSpreadEvent.Cause.FIRE);
        var leaves = new LeavesDecayEvent(block, oldType);
        var dispense = new BlockDispenseEvent(sourceBlock, BlockFace.NORTH, stack("minecraft:arrow", 1));
        dispense.setItem(stack("minecraft:spectral_arrow", 1));
        dispense.setCancelled(true);

        assertThat(burn.sourceBlock()).isSameAs(sourceBlock);
        assertThat(burn.cancelled()).isTrue();
        assertThat(fade.cause()).isEqualTo(BlockFadeEvent.Cause.MELT);
        assertThat(grow.cause()).isEqualTo(BlockGrowEvent.Cause.BONEMEAL);
        assertThat(ignite.sourceBlock()).contains(sourceBlock);
        assertThat(ignite.cancelled()).isTrue();
        assertThat(spread.sourceType()).isSameAs(oldType);
        assertThat(spread.newType()).isSameAs(newType);
        assertThat(leaves.blockType()).isSameAs(oldType);
        assertThat(dispense.direction()).isEqualTo(BlockFace.NORTH);
        assertThat(dispense.item().type().key()).isEqualTo(Key.key("minecraft:spectral_arrow"));
        assertThat(dispense.cancelled()).isTrue();
    }

    @Test
    void blockFineEventsCarryPistonAndFluidDetails() {
        var sourceBlock = proxy(Block.class);
        var block = proxy(Block.class);
        var affected = java.util.List.of(proxy(Block.class), proxy(Block.class));

        var extend = new BlockPistonExtendEvent(sourceBlock, BlockFace.EAST, affected);
        extend.setCancelled(true);
        var push = new BlockPistonPushEvent(sourceBlock, BlockFace.SOUTH, affected);
        push.setCancelled(true);
        var retract = new BlockPistonRetractEvent(sourceBlock, BlockFace.WEST, affected);
        var fluid = new FluidFlowEvent(sourceBlock, block, Key.key("minecraft:water"), BlockFace.DOWN);
        fluid.setCancelled(true);

        assertThat(extend.block()).isSameAs(sourceBlock);
        assertThat(extend.direction()).isEqualTo(BlockFace.EAST);
        assertThat(extend.affectedBlocks()).hasSize(2);
        assertThat(extend.cancelled()).isTrue();
        assertThat(push).isInstanceOf(BlockPistonExtendEvent.class);
        assertThat(push.direction()).isEqualTo(BlockFace.SOUTH);
        assertThat(push.cancelled()).isTrue();
        assertThat(retract.direction()).isEqualTo(BlockFace.WEST);
        assertThat(fluid.sourceBlock()).isSameAs(sourceBlock);
        assertThat(fluid.block()).isSameAs(block);
        assertThat(fluid.fluid()).isEqualTo(Key.key("minecraft:water"));
        assertThat(fluid.direction()).isEqualTo(BlockFace.DOWN);
        assertThat(fluid.cancelled()).isTrue();
    }

    @Test
    void blockEcosystemEventsExposeMutableExplosionRedstoneAndSigns() {
        var player = proxy(Player.class);
        var block = proxy(Block.class);
        var affected = new java.util.ArrayList<>(List.of(proxy(Block.class), proxy(Block.class)));

        var explode = new BlockExplodeEvent(block, affected);
        explode.affectedBlocks().removeFirst();
        explode.setCancelled(true);
        var redstone = new BlockRedstoneEvent(block, 20, -4);
        redstone.setNewCurrent(99);
        var sign = new SignChangeEvent(player, block, true, List.of("[fand]", "old"));
        sign.setLine(1, "new");
        sign.setCancelled(true);

        assertThat(explode.block()).isSameAs(block);
        assertThat(explode.affectedBlocks()).hasSize(1);
        assertThat(explode.cancelled()).isTrue();
        assertThat(redstone.oldCurrent()).isEqualTo(15);
        assertThat(redstone.newCurrent()).isEqualTo(15);
        assertThat(sign.player()).isSameAs(player);
        assertThat(sign.frontText()).isTrue();
        assertThat(sign.lines()).containsExactly("[fand]", "new");
        assertThat(sign.cancelled()).isTrue();
    }

    @Test
    void craftingEventsCarryRecipeAndMutableResult() {
        var player = proxy(Player.class);
        var inventory = proxy(Inventory.class);
        var recipe = proxy(Recipe.class);
        var stone = stack("minecraft:stone", 1);
        var diamond = stack("minecraft:diamond", 1);

        var prepare = new PrepareItemCraftEvent(player, inventory, recipe, stone);
        prepare.setResult(diamond);
        var craft = new CraftItemEvent(player, inventory, recipe, diamond, ClickType.QUICK_MOVE);
        craft.setCancelled(true);

        assertThat(prepare.player()).isSameAs(player);
        assertThat(prepare.inventory()).isSameAs(inventory);
        assertThat(prepare.recipe()).contains(recipe);
        assertThat(prepare.result()).isSameAs(diamond);
        assertThat(craft.result()).isSameAs(diamond);
        assertThat(craft.clickType()).isEqualTo(ClickType.QUICK_MOVE);
        assertThat(craft.cancelled()).isTrue();
    }

    @Test
    void workstationEventsCarryMutableResultsAndCancellation() {
        var player = proxy(Player.class);
        var inventory = proxy(Inventory.class);
        var block = proxy(Block.class);
        var recipe = proxy(Recipe.class);
        var stone = stack("minecraft:stone", 1);
        var diamond = stack("minecraft:diamond", 1);
        var coal = stack("minecraft:coal", 1);
        var offer = new EnchantmentOffer(0, -3, Key.key("minecraft:sharpness"), -2);
        offer.setCost(5);
        offer.setLevel(2);

        var prepareEnchant = new PrepareItemEnchantEvent(player, inventory, stone, 15, List.of(offer));
        var enchant = new EnchantItemEvent(player, inventory, stone, diamond, 0, 3, 2, List.of(offer));
        enchant.setResultItem(stone);
        enchant.setCancelled(true);
        var anvil = new PrepareAnvilEvent(player, inventory, stone, diamond, stone, 7, "Demo");
        anvil.setResult(diamond);
        anvil.setCost(4);
        var smithing = new PrepareSmithingEvent(player, inventory, recipe, stone, stone, diamond, stone);
        smithing.setResult(diamond);
        var burn = new FurnaceBurnEvent(block, inventory, coal, -1);
        burn.setBurnTime(200);
        burn.setCancelled(true);
        var smelt = new FurnaceSmeltEvent(block, inventory, recipe, stone, diamond);
        smelt.setResult(stone);
        smelt.setCancelled(true);
        var extract = new FurnaceExtractEvent(player, inventory, diamond, 3);
        var brewingFuel = new BrewingStandFuelEvent(block, inventory, coal, 999, -1);
        brewingFuel.setConsumeAmount(2);
        brewingFuel.setCancelled(true);
        var brew = new BrewEvent(block, inventory, stone, List.of(diamond));
        brew.results().add(stone);
        brew.setCancelled(true);

        assertThat(offer.cost()).isEqualTo(5);
        assertThat(offer.level()).isEqualTo(2);
        assertThat(prepareEnchant.bookshelfPower()).isEqualTo(15);
        assertThat(prepareEnchant.offers()).singleElement().isSameAs(offer);
        assertThat(enchant.resultItem()).isSameAs(stone);
        assertThat(enchant.cancelled()).isTrue();
        assertThat(anvil.result()).isSameAs(diamond);
        assertThat(anvil.cost()).isEqualTo(4);
        assertThat(anvil.renameText()).contains("Demo");
        assertThat(smithing.recipe()).contains(recipe);
        assertThat(smithing.result()).isSameAs(diamond);
        assertThat(burn.burnTime()).isEqualTo(200);
        assertThat(burn.cancelled()).isTrue();
        assertThat(smelt.result()).isSameAs(stone);
        assertThat(smelt.cancelled()).isTrue();
        assertThat(extract.amount()).isEqualTo(3);
        assertThat(brewingFuel.fuelPower()).isEqualTo(127);
        assertThat(brewingFuel.consumeAmount()).isEqualTo(2);
        assertThat(brewingFuel.cancelled()).isTrue();
        assertThat(brew.results()).hasSize(2);
        assertThat(brew.cancelled()).isTrue();
    }

    @Test
    void worldLifecycleEventsCarryWorld() {
        var world = new TestWorld(Key.key("minecraft:overworld"));

        assertThat(new WorldLoadEvent(world).world()).isSameAs(world);
        assertThat(new WorldUnloadEvent(world).world()).isSameAs(world);
        assertThat(new WorldSaveEvent(world).world()).isSameAs(world);
        assertThat(new ChunkLoadEvent(world, 1, -2).chunkX()).isEqualTo(1);
        assertThat(new ChunkUnloadEvent(world, 1, -2).chunkZ()).isEqualTo(-2);
        var weather = new WeatherChangeEvent(world, false, true);
        var thunder = new ThunderChangeEvent(world, false, true);
        thunder.setCancelled(true);
        assertThat(weather.toStorm()).isTrue();
        assertThat(thunder.toThundering()).isTrue();
        assertThat(thunder.cancelled()).isTrue();
    }

    @Test
    void entityEcosystemEventsCarryMutableExplosionProjectilesAndTransforms() {
        var entity = proxy(Entity.class);
        var shooter = proxy(LivingEntity.class);
        var projectile = proxy(Entity.class);
        var block = proxy(Block.class);
        var location = location("minecraft:overworld", 1, 64, 1);
        var bow = stack("minecraft:bow", 1);
        var arrow = stack("minecraft:arrow", 1);
        var affected = new java.util.ArrayList<>(List.of(block, proxy(Block.class)));

        var explode = new EntityExplodeEvent(entity, location, affected);
        explode.affectedBlocks().removeLast();
        explode.setCancelled(true);
        var launch = new ProjectileLaunchEvent(projectile, entity, arrow);
        launch.setCancelled(true);
        var shoot = new EntityShootBowEvent(shooter, bow, arrow, projectile, -1.0F);
        shoot.setCancelled(true);
        var resurrect = new EntityResurrectEvent(shooter, stack("minecraft:totem_of_undying", 1), EntityResurrectEvent.Hand.OFF_HAND);
        resurrect.setCancelled(true);
        var transform = new EntityTransformEvent(entity, Key.key("minecraft:zombie_villager"), EntityTransformEvent.Cause.CONVERSION);
        transform.setCancelled(true);

        assertThat(explode.location()).isSameAs(location);
        assertThat(explode.affectedBlocks()).containsExactly(block);
        assertThat(explode.cancelled()).isTrue();
        assertThat(launch.shooter()).contains(entity);
        assertThat(launch.cancelled()).isTrue();
        assertThat(shoot.force()).isZero();
        assertThat(shoot.cancelled()).isTrue();
        assertThat(resurrect.hand()).isEqualTo(EntityResurrectEvent.Hand.OFF_HAND);
        assertThat(resurrect.cancelled()).isTrue();
        assertThat(transform.targetType()).isEqualTo(Key.key("minecraft:zombie_villager"));
        assertThat(transform.cause()).isEqualTo(EntityTransformEvent.Cause.CONVERSION);
        assertThat(transform.cancelled()).isTrue();
    }

    @Test
    void serverAndWorldControlEventsCarryMutableState() {
        var address = InetSocketAddress.createUnresolved("127.0.0.1", 25565);
        var login = new PlayerLoginEvent(
                java.util.UUID.randomUUID(),
                "Steve",
                address,
                PlayerLoginEvent.Result.ALLOWED,
                Component.text("ok"));
        login.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text("no"));
        var ping = new ServerListPingEvent(Component.text("old"), 2, 20, false);
        ping.setMotd(Component.text("new"));
        ping.setOnlinePlayers(-1);
        ping.setMaxPlayers(40);
        ping.setHidePlayers(true);
        var previous = location("minecraft:overworld", 0, 64, 0);
        var next = location("minecraft:overworld", 10, 70, 10);
        var spawn = new SpawnChangeEvent(previous, next);
        spawn.setNewSpawn(previous);
        spawn.setCancelled(true);
        var skip = new TimeSkipEvent(previous.world(), TimeSkipEvent.Cause.SLEEP, 1000, 24000);
        skip.setToTime(26000);
        skip.setCancelled(true);

        assertThat(login.address()).isSameAs(address);
        assertThat(login.result()).isEqualTo(PlayerLoginEvent.Result.KICK_OTHER);
        assertThat(login.kickMessage()).isEqualTo(Component.text("no"));
        assertThat(ping.motd()).isEqualTo(Component.text("new"));
        assertThat(ping.onlinePlayers()).isZero();
        assertThat(ping.maxPlayers()).isEqualTo(40);
        assertThat(ping.hidePlayers()).isTrue();
        assertThat(spawn.previousSpawn()).isSameAs(previous);
        assertThat(spawn.newSpawn()).isSameAs(previous);
        assertThat(spawn.cancelled()).isTrue();
        assertThat(skip.world()).isSameAs(previous.world());
        assertThat(skip.toTime()).isEqualTo(26000);
        assertThat(skip.cancelled()).isTrue();
    }

    @Test
    void expandedEcosystemEventsCarryTypedState() {
        var player = proxy(Player.class);
        var entity = proxy(Entity.class);
        var vehicle = proxy(Entity.class);
        var inventory = proxy(Inventory.class);
        var block = proxy(Block.class);
        var from = location("minecraft:overworld", 0, 64, 0);
        var to = location("minecraft:the_nether", 1, 80, 1);
        var stone = stack("minecraft:stone", 3);
        var diamond = stack("minecraft:diamond", 1);

        var tab = new TabCompleteEvent(player, "/fand", List.of("fanddemo"));
        tab.completions().add("fandselftest");
        tab.setCancelled(true);
        var playerPortal = new PlayerPortalEvent(player, from, to);
        playerPortal.setTo(from);
        playerPortal.setCancelled(true);
        var entityPortal = new EntityPortalEvent(entity, from, to);
        entityPortal.setCancelled(true);
        var changedWorld = new PlayerChangedWorldEvent(player, from.world(), to.world());
        var locale = new PlayerLocaleChangeEvent(player, "EN_US", "ZH_CN");
        var brand = new PlayerClientBrandEvent(player, "fabric");
        var pack = new PlayerResourcePackStatusEvent(
                player,
                java.util.UUID.randomUUID(),
                PlayerResourcePackStatusEvent.Status.ACCEPTED);

        var itemSpawn = new ItemSpawnEvent(entity, stone);
        itemSpawn.setItem(diamond);
        itemSpawn.setCancelled(true);
        var itemDespawn = new ItemDespawnEvent(entity, stone, -1);
        itemDespawn.setCancelled(true);
        var itemMerge = new ItemMergeEvent(entity, vehicle, stone, diamond);
        itemMerge.setCancelled(true);
        var hangingPlace = new HangingPlaceEvent(player, entity, block, BlockFace.NORTH, stone);
        hangingPlace.setCancelled(true);
        var hangingBreak = new HangingBreakEvent(entity, vehicle, HangingBreakEvent.Cause.ENTITY);
        hangingBreak.setCancelled(true);
        var frame = new PlayerItemFrameChangeEvent(player, entity, PlayerItemFrameChangeEvent.Action.ROTATE, stone, 9);
        frame.setItem(diamond);
        frame.setRotation(-1);
        frame.setCancelled(true);

        var creative = new InventoryCreativeEvent(player, -1, true, stone);
        creative.setItem(diamond);
        creative.setCancelled(true);
        var prepareTrade = new PrepareTradeEvent(player, inventory, stone, ItemStack.EMPTY, diamond, -1);
        prepareTrade.setResult(stone);
        var trade = new InventoryTradeEvent(player, inventory, stone, ItemStack.EMPTY, diamond, 2);
        trade.setCancelled(true);

        var vehicleCreate = new VehicleCreateEvent(vehicle);
        vehicleCreate.setCancelled(true);
        var vehicleDestroy = new VehicleDestroyEvent(vehicle, entity);
        vehicleDestroy.setCancelled(true);
        var vehicleEnter = new VehicleEnterEvent(vehicle, player);
        vehicleEnter.setCancelled(true);
        var vehicleExit = new VehicleExitEvent(vehicle, player);
        vehicleExit.setCancelled(true);
        var vehicleMove = new VehicleMoveEvent(vehicle, from, to);

        assertThat(tab.completions()).containsExactly("fanddemo", "fandselftest");
        assertThat(tab.cancelled()).isTrue();
        assertThat(playerPortal.to()).isSameAs(from);
        assertThat(playerPortal.cancelled()).isTrue();
        assertThat(entityPortal.cancelled()).isTrue();
        assertThat(changedWorld.fromWorld()).isSameAs(from.world());
        assertThat(changedWorld.toWorld()).isSameAs(to.world());
        assertThat(locale.oldLocale()).isEqualTo("en_us");
        assertThat(locale.newLocale()).isEqualTo("zh_cn");
        assertThat(brand.brand()).isEqualTo("fabric");
        assertThat(pack.status()).isEqualTo(PlayerResourcePackStatusEvent.Status.ACCEPTED);
        assertThat(itemSpawn.item()).isSameAs(diamond);
        assertThat(itemSpawn.cancelled()).isTrue();
        assertThat(itemDespawn.age()).isZero();
        assertThat(itemDespawn.cancelled()).isTrue();
        assertThat(itemMerge.source()).isSameAs(vehicle);
        assertThat(itemMerge.cancelled()).isTrue();
        assertThat(hangingPlace.face()).isEqualTo(BlockFace.NORTH);
        assertThat(hangingPlace.cancelled()).isTrue();
        assertThat(hangingBreak.cause()).isEqualTo(HangingBreakEvent.Cause.ENTITY);
        assertThat(hangingBreak.cancelled()).isTrue();
        assertThat(frame.item()).isSameAs(diamond);
        assertThat(frame.rotation()).isEqualTo(7);
        assertThat(frame.cancelled()).isTrue();
        assertThat(creative.item()).isSameAs(diamond);
        assertThat(creative.cancelled()).isTrue();
        assertThat(prepareTrade.result()).isSameAs(stone);
        assertThat(prepareTrade.villagerExperience()).isZero();
        assertThat(trade.cancelled()).isTrue();
        assertThat(vehicleCreate.cancelled()).isTrue();
        assertThat(vehicleDestroy.attacker()).contains(entity);
        assertThat(vehicleDestroy.cancelled()).isTrue();
        assertThat(vehicleEnter.cancelled()).isTrue();
        assertThat(vehicleExit.cancelled()).isTrue();
        assertThat(vehicleMove.to()).isSameAs(to);
    }

    @Test
    void completeEcosystemEventsCarryTypedState() {
        var player = proxy(Player.class);
        var entity = proxy(Entity.class);
        var living = proxy(LivingEntity.class);
        var inventory = proxy(Inventory.class);
        var block = proxy(Block.class);
        var sourceBlock = proxy(Block.class);
        var world = new TestWorld(Key.key("minecraft:overworld"));
        var location = location("minecraft:overworld", 1, 64, 1);
        var stone = stack("minecraft:stone", 3);
        var diamond = stack("minecraft:diamond", 1);
        var oldType = new TestBlockType(Key.key("minecraft:water"));
        var newType = new TestBlockType(Key.key("minecraft:stone"));

        var portal = new PortalCreateEvent(world, List.of(block), PortalCreateEvent.Type.NETHER, PortalCreateEvent.Cause.FIRE);
        portal.setCancelled(true);
        var form = new BlockFormEvent(block, oldType, newType, BlockFormEvent.Cause.LAVA_INTERACTION);
        form.setCancelled(true);
        var fromTo = new BlockFromToEvent(sourceBlock, block, oldType, newType, BlockFace.DOWN, BlockFromToEvent.Cause.FLUID_FLOW);
        fromTo.setCancelled(true);
        var fertilize = new BlockFertilizeEvent(player, block, stone, BlockFertilizeEvent.Cause.BONE_MEAL);
        fertilize.setCancelled(true);
        var sponge = new SpongeAbsorbEvent(block, List.of(sourceBlock));
        sponge.setCancelled(true);
        var cauldron = new CauldronLevelChangeEvent(
                block,
                oldType,
                newType,
                -1,
                9,
                entity,
                CauldronLevelChangeEvent.Cause.BUCKET_EMPTY);
        cauldron.setCancelled(true);

        var hopperMove = new HopperMoveItemEvent(inventory, inventory, inventory, location, stone, true);
        hopperMove.setItem(diamond);
        hopperMove.setCancelled(true);
        var hopperPickup = new HopperPickupItemEvent(inventory, location, entity, stone);
        hopperPickup.setItem(diamond);
        hopperPickup.setCancelled(true);

        var entityPickup = new EntityPickupItemEvent(living, stone);
        entityPickup.setItem(diamond);
        entityPickup.setCancelled(true);
        var entityDrop = new EntityDropItemEvent(entity, location, stone);
        entityDrop.setItem(diamond);
        entityDrop.setCancelled(true);
        var changeBlock = new EntityChangeBlockEvent(entity, block, oldType, newType, EntityChangeBlockEvent.Cause.PLACE);
        changeBlock.setCancelled(true);
        var potionTargets = new java.util.LinkedHashMap<LivingEntity, Double>();
        potionTargets.put(living, 0.75);
        var potionSplash = new PotionSplashEvent(entity, stone, location, entity, potionTargets);
        potionSplash.affectedEntities().put(proxy(LivingEntity.class), 0.25);
        potionSplash.setCancelled(true);
        var lingering = new LingeringPotionSplashEvent(entity, stone, location, null);
        lingering.setCancelled(true);
        var fish = new PlayerFishEvent(player, entity, PlayerFishEvent.State.CAUGHT_FISH, null, List.of(stone));
        fish.setCancelled(true);
        var shear = new PlayerShearEntityEvent(player, entity, PlayerInteractEvent.Hand.MAIN_HAND, stone);
        shear.setCancelled(true);
        var leash = new PlayerLeashEntityEvent(player, entity, entity, PlayerLeashEntityEvent.Cause.FENCE);
        leash.setCancelled(true);
        var unleash = new PlayerUnleashEntityEvent(player, entity, entity, true);
        unleash.setCancelled(true);
        var permission = new PermissionCheckEvent(proxy(io.fand.api.permission.PermissionSubject.class), "fand.test", false);
        permission.allow();

        assertThat(portal.blocks()).containsExactly(block);
        assertThat(portal.type()).isEqualTo(PortalCreateEvent.Type.NETHER);
        assertThat(portal.cancelled()).isTrue();
        assertThat(form.cause()).isEqualTo(BlockFormEvent.Cause.LAVA_INTERACTION);
        assertThat(form.cancelled()).isTrue();
        assertThat(fromTo.sourceBlock()).isSameAs(sourceBlock);
        assertThat(fromTo.direction()).isEqualTo(BlockFace.DOWN);
        assertThat(fromTo.cancelled()).isTrue();
        assertThat(fertilize.player()).contains(player);
        assertThat(fertilize.item()).isSameAs(stone);
        assertThat(fertilize.cancelled()).isTrue();
        assertThat(sponge.absorbedBlocks()).containsExactly(sourceBlock);
        assertThat(sponge.cancelled()).isTrue();
        assertThat(cauldron.oldLevel()).isZero();
        assertThat(cauldron.newLevel()).isEqualTo(3);
        assertThat(cauldron.entity()).contains(entity);
        assertThat(cauldron.cancelled()).isTrue();
        assertThat(hopperMove.item()).isSameAs(diamond);
        assertThat(hopperMove.hopperInitiated()).isTrue();
        assertThat(hopperMove.cancelled()).isTrue();
        assertThat(hopperPickup.itemEntity()).isSameAs(entity);
        assertThat(hopperPickup.item()).isSameAs(diamond);
        assertThat(hopperPickup.cancelled()).isTrue();
        assertThat(entityPickup.entity()).isSameAs(living);
        assertThat(entityPickup.item()).isSameAs(diamond);
        assertThat(entityPickup.cancelled()).isTrue();
        assertThat(entityDrop.item()).isSameAs(diamond);
        assertThat(entityDrop.location()).isSameAs(location);
        assertThat(entityDrop.cancelled()).isTrue();
        assertThat(changeBlock.newType()).isSameAs(newType);
        assertThat(changeBlock.cancelled()).isTrue();
        assertThat(potionSplash.source()).contains(entity);
        assertThat(potionSplash.affectedEntities()).hasSize(2);
        assertThat(potionSplash.cancelled()).isTrue();
        assertThat(lingering.location()).isSameAs(location);
        assertThat(lingering.cancelled()).isTrue();
        assertThat(fish.state()).isEqualTo(PlayerFishEvent.State.CAUGHT_FISH);
        assertThat(fish.drops()).containsExactly(stone);
        assertThat(fish.cancelled()).isTrue();
        assertThat(shear.tool()).isSameAs(stone);
        assertThat(shear.cancelled()).isTrue();
        assertThat(leash.cause()).isEqualTo(PlayerLeashEntityEvent.Cause.FENCE);
        assertThat(leash.cancelled()).isTrue();
        assertThat(unleash.dropLead()).isTrue();
        assertThat(unleash.cancelled()).isTrue();
        assertThat(permission.result()).contains(true);
        assertThat(permission.effectiveResult()).isTrue();
    }

    @Test
    void newlyAddedBlockInventoryAndWorldEventsCarryMutableFields() {
        var player = proxy(Player.class);
        var block = proxy(Block.class);
        var otherBlock = proxy(Block.class);
        var placedType = new TestBlockType(Key.key("minecraft:oak_door"));
        var replacedType = new TestBlockType(Key.key("minecraft:air"));
        var inventory = proxy(Inventory.class);
        var source = stack("minecraft:raw_iron", 1);
        var result = stack("minecraft:iron_ingot", 1);
        var replacement = stack("minecraft:gold_ingot", 1);
        var recipe = new TestRecipe(Key.key("minecraft:iron_ingot_from_smelting_raw_iron"), RecipeType.SMELTING, result);
        var location = location("minecraft:overworld", 10, 64, 10);

        var canBuild = new BlockCanBuildEvent(player, block, placedType, source, false);
        canBuild.setBuildable(true);
        var multiPlace = new BlockMultiPlaceEvent(player, block, placedType, replacedType, List.of(block, otherBlock));
        multiPlace.setCancelled(true);
        var cook = new BlockCookEvent(block, inventory, source, result);
        cook.setResult(replacement);
        cook.setCancelled(true);
        var smelt = new FurnaceSmeltEvent(block, inventory, recipe, source, result);
        smelt.setResult(replacement);
        smelt.setCancelled(true);
        var startSmelt = new FurnaceStartSmeltEvent(block, inventory, recipe, source, -20);
        startSmelt.setTotalCookTime(80);
        startSmelt.setCancelled(true);
        var grow = new StructureGrowEvent(location, player, true, List.of(block, otherBlock));
        grow.setCancelled(true);

        assertThat(canBuild.player()).contains(player);
        assertThat(canBuild.block()).isSameAs(block);
        assertThat(canBuild.blockType()).isSameAs(placedType);
        assertThat(canBuild.item()).isSameAs(source);
        assertThat(canBuild.buildable()).isTrue();
        assertThat(multiPlace).isInstanceOf(BlockPlaceEvent.class);
        assertThat(multiPlace.blocks()).containsExactly(block, otherBlock);
        assertThat(multiPlace.placedType()).isSameAs(placedType);
        assertThat(multiPlace.replacedType()).isSameAs(replacedType);
        assertThat(multiPlace.cancelled()).isTrue();
        assertThat(cook.result()).isSameAs(replacement);
        assertThat(cook.cancelled()).isTrue();
        assertThat(smelt).isInstanceOf(BlockCookEvent.class);
        assertThat(smelt.recipe()).contains(recipe);
        assertThat(smelt.result()).isSameAs(replacement);
        assertThat(smelt.cancelled()).isTrue();
        assertThat(startSmelt.recipe()).contains(recipe);
        assertThat(startSmelt.totalCookTime()).isEqualTo(80);
        assertThat(startSmelt.cancelled()).isTrue();
        assertThat(grow.location()).isSameAs(location);
        assertThat(grow.player()).contains(player);
        assertThat(grow.fromBonemeal()).isTrue();
        assertThat(grow.blocks()).containsExactly(block, otherBlock);
        assertThat(grow.cancelled()).isTrue();
    }

    private static <T> T proxy(Class<T> type) {
        Object instance = Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> type.getSimpleName() + " proxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> args != null && args.length == 1 && proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
        return type.cast(instance);
    }

    private static ItemStack stack(String key, int amount) {
        return new ItemStack(new TestItemType(Key.key(key), 64), amount);
    }

    private static Location location(String key, double x, double y, double z) {
        return new Location(new TestWorld(Key.key(key)), x, y, z, 0.0F, 0.0F);
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private record TestBlockType(Key key) implements BlockType {
    }

    private record TestRecipe(Key key, RecipeType type, ItemStack result) implements Recipe {
        @Override
        public Optional<String> group() {
            return Optional.empty();
        }
    }

    private record TestWorld(Key key) implements World {
        @Override
        public long seed() {
            return 0;
        }

        @Override
        public long gameTime() {
            return 0;
        }

        @Override
        public CompletableFuture<Void> setGameTime(long ticks) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public long time() {
            return 0;
        }

        @Override
        public CompletableFuture<Void> setTime(long ticks) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Difficulty difficulty() {
            return Difficulty.NORMAL;
        }

        @Override
        public CompletableFuture<Void> setDifficulty(Difficulty difficulty) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean storm() {
            return false;
        }

        @Override
        public CompletableFuture<Void> setStorm(boolean storm) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean thundering() {
            return false;
        }

        @Override
        public CompletableFuture<Void> setThundering(boolean thundering) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public WorldBorder worldBorder() {
            return TestWorldBorder.INSTANCE;
        }

        @Override
        public CompletableFuture<Boolean> save() {
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public java.util.Collection<? extends Player> players() {
            return java.util.List.of();
        }

        @Override
        public void playSound(io.fand.api.world.Location location, io.fand.api.world.sound.SoundEffect sound) {
        }

        @Override
        public void spawnParticle(
                io.fand.api.world.Location location,
                io.fand.api.world.particle.ParticleEffect effect,
                io.fand.api.world.particle.ParticleEmission emission) {
        }

        @Override
        public Iterable<? extends net.kyori.adventure.audience.Audience> audiences() {
            return java.util.List.of();
        }

        @Override
        public io.fand.api.block.Block blockAt(int x, int y, int z) {
            throw new UnsupportedOperationException();
        }
    }

    private enum TestWorldBorder implements WorldBorder {
        INSTANCE;

        @Override
        public double centerX() {
            return 0;
        }

        @Override
        public double centerZ() {
            return 0;
        }

        @Override
        public void setCenter(double x, double z) {
        }

        @Override
        public double size() {
            return 0;
        }

        @Override
        public double targetSize() {
            return 0;
        }

        @Override
        public long remainingTransitionTicks() {
            return 0;
        }

        @Override
        public void setSize(double size) {
        }

        @Override
        public void setSize(double size, Duration transition) {
        }

        @Override
        public int warningDistance() {
            return 0;
        }

        @Override
        public void setWarningDistance(int blocks) {
        }

        @Override
        public int warningTime() {
            return 0;
        }

        @Override
        public void setWarningTime(int seconds) {
        }

        @Override
        public double damageBuffer() {
            return 0;
        }

        @Override
        public void setDamageBuffer(double blocks) {
        }

        @Override
        public double damageAmount() {
            return 0;
        }

        @Override
        public void setDamageAmount(double damagePerBlock) {
        }

        @Override
        public boolean contains(double x, double z) {
            return true;
        }
    }
}
