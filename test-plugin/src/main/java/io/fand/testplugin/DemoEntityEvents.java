package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.entity.Player;
import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.entity.EntityBreedEvent;
import io.fand.api.event.entity.EntityChangeBlockEvent;
import io.fand.api.event.entity.EntityCombustEvent;
import io.fand.api.event.entity.EntityDamageByEntityEvent;
import io.fand.api.event.entity.EntityDeathEvent;
import io.fand.api.event.entity.EntityDismountEvent;
import io.fand.api.event.entity.EntityDropItemEvent;
import io.fand.api.event.entity.EntityExplodeEvent;
import io.fand.api.event.entity.EntityMountEvent;
import io.fand.api.event.entity.EntityPickupItemEvent;
import io.fand.api.event.entity.EntityPortalEvent;
import io.fand.api.event.entity.EntityPotionEffectEvent;
import io.fand.api.event.entity.EntityRegainHealthEvent;
import io.fand.api.event.entity.EntityRemoveEvent;
import io.fand.api.event.entity.EntityResurrectEvent;
import io.fand.api.event.entity.EntityShootBowEvent;
import io.fand.api.event.entity.EntitySpawnEvent;
import io.fand.api.event.entity.EntityTameEvent;
import io.fand.api.event.entity.EntityTargetEvent;
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
import io.fand.api.event.vehicle.VehicleCreateEvent;
import io.fand.api.event.vehicle.VehicleDestroyEvent;
import io.fand.api.event.vehicle.VehicleEnterEvent;
import io.fand.api.event.vehicle.VehicleExitEvent;
import io.fand.api.event.vehicle.VehicleMoveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import io.fand.api.plugin.PluginContext;
import org.slf4j.Logger;

final class DemoEntityEvents implements Listener {

    private final PluginContext context;
    private final Logger logger;

    DemoEntityEvents(PluginContext context) {
        this.context = context;
        this.logger = context.logger();
    }

    @Subscribe
    public void onEntityDeath(EntityDeathEvent event) {
        if (context.config().getBoolean("features.log-entity-deaths", false)) {
            logger.info("{} died from {}", event.entity().type().asString(), event.cause());
        }
    }

    @Subscribe
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (context.config().getBoolean("features.log-entity-spawns", false)) {
            logger.info("Entity spawned: {} cause={} at {}",
                    event.entity().type().asString(), event.cause(), compactLocation(event.entity().location()));
        }
    }

    @Subscribe
    public void onItemSpawn(ItemSpawnEvent event) {
        if (context.config().getBoolean("features.log-item-events", false)) {
            logger.info("Item spawned: {} at {}", stackName(event.item()), compactLocation(event.entity().location()));
        }
    }

    @Subscribe
    public void onItemDespawn(ItemDespawnEvent event) {
        if (context.config().getBoolean("features.log-item-events", false)) {
            logger.info("Item despawn: {} age={}", stackName(event.item()), event.age());
        }
    }

    @Subscribe
    public void onItemMerge(ItemMergeEvent event) {
        if (context.config().getBoolean("features.log-item-events", false)) {
            logger.info("Item merge: {} <- {}", stackName(event.targetItem()), stackName(event.sourceItem()));
        }
    }

    @Subscribe
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (context.config().getBoolean("features.log-item-events", false)) {
            logger.info("Entity picked up item: {} item={}",
                    event.entity().type().asString(), stackName(event.item()));
        }
    }

    @Subscribe
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (context.config().getBoolean("features.log-item-events", false)) {
            logger.info("Entity dropped item: {} item={} at {}",
                    event.entity().type().asString(),
                    stackName(event.item()),
                    compactLocation(event.location()));
        }
    }

    @Subscribe
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity changed block: {} {},{},{} {} -> {} cause={}",
                    event.entity().type().asString(),
                    event.block().x(), event.block().y(), event.block().z(),
                    blockName(event.oldType()), blockName(event.newType()), event.cause());
        }
    }

    @Subscribe
    public void onEntityRemove(EntityRemoveEvent event) {
        if (context.config().getBoolean("features.log-entity-removes", false)) {
            logger.info("Entity removed: {} cause={}", event.entity().type().asString(), event.cause());
        }
    }

    @Subscribe
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (context.config().getBoolean("features.log-entity-teleports", false)) {
            logger.info("Entity teleport: {} {} -> {}",
                    event.entity().type().asString(), compactLocation(event.from()), compactLocation(event.to()));
        }
    }

    @Subscribe
    public void onEntityPortal(EntityPortalEvent event) {
        if (context.config().getBoolean("features.log-entity-teleports", false)) {
            logger.info("Entity portal: {} {} -> {}",
                    event.entity().type().asString(), compactLocation(event.from()), compactLocation(event.to()));
        }
    }

    @Subscribe
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (context.config().getBoolean("protections.limit-explosion-radius", true) && event.radius() > 8.0F) {
            event.setRadius(8.0F);
            logger.info("Limited explosion at {} to radius {}", compactLocation(event.location()), event.radius());
        }
    }

    @Subscribe
    public void onEntityExplode(EntityExplodeEvent event) {
        if (context.config().getBoolean("protections.limit-explosion-block-damage", true)
                && event.affectedBlocks().size() > 512) {
            event.affectedBlocks().subList(512, event.affectedBlocks().size()).clear();
        }
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity explosion: {} at {} affected={}",
                    event.entity().type().asString(), compactLocation(event.location()), event.affectedBlocks().size());
        }
    }

    @Subscribe
    public void onEntityCombust(EntityCombustEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity combust: {} duration={} cause={}",
                    event.entity().type().asString(), trim(event.durationSeconds()), event.cause());
        }
    }

    @Subscribe
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity heal: {} amount={} cause={}",
                    event.entity().type().asString(), trim(event.amount()), event.cause());
        }
    }

    @Subscribe
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity attacked: {} <- {} cause={} amount={}",
                    event.entity().type().asString(),
                    event.damager().type().asString(),
                    event.cause(),
                    trim(event.amount()));
        }
    }

    @Subscribe
    public void onProjectileHit(ProjectileHitEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Projectile hit: {} type={} block={} entity={}",
                    event.projectile().type().asString(),
                    event.hitType(),
                    event.hitBlock().map(block -> blockName(block.type())).orElse("none"),
                    event.hitEntity().map(entity -> entity.type().asString()).orElse("none"));
        }
    }

    @Subscribe
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Projectile launch: {} shooter={} item={}",
                    event.projectile().type().asString(),
                    event.shooter().map(entity -> entity.type().asString()).orElse("none"),
                    stackName(event.item()));
        }
    }

    @Subscribe
    public void onShootBow(EntityShootBowEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Bow shot: {} projectile={} force={} consumable={}",
                    event.shooter().type().asString(),
                    event.projectile().type().asString(),
                    trim(event.force()),
                    stackName(event.consumable()));
        }
    }

    @Subscribe
    public void onEntityTarget(EntityTargetEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity target: {} -> {} cause={}",
                    event.entity().type().asString(),
                    event.target().map(entity -> entity.type().asString()).orElse("none"),
                    event.cause());
        }
    }

    @Subscribe
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity effect: {} {} action={} source={}",
                    event.entity().type().asString(),
                    event.effect().asString(),
                    event.action(),
                    event.source().map(entity -> entity.type().asString()).orElse("none"));
        }
    }

    @Subscribe
    public void onPotionSplash(PotionSplashEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Potion splash: {} at {} affected={} source={}",
                    stackName(event.item()),
                    compactLocation(event.location()),
                    event.affectedEntities().size(),
                    event.source().map(entity -> entity.type().asString()).orElse("none"));
        }
    }

    @Subscribe
    public void onLingeringPotionSplash(LingeringPotionSplashEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Lingering potion: {} at {} source={}",
                    stackName(event.item()),
                    compactLocation(event.location()),
                    event.source().map(entity -> entity.type().asString()).orElse("none"));
        }
    }

    @Subscribe
    public void onEntityMount(EntityMountEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity mount: {} -> {}",
                    event.entity().type().asString(), event.vehicle().type().asString());
        }
    }

    @Subscribe
    public void onEntityDismount(EntityDismountEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity dismount: {} <- {}",
                    event.entity().type().asString(), event.vehicle().type().asString());
        }
    }

    @Subscribe
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (event.entity() instanceof Player player) {
            player.sendMessage(Component.text("Resurrection event observed with " + stackName(event.item()) + ".", NamedTextColor.GOLD));
        }
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity resurrect: {} hand={} item={}",
                    event.entity().type().asString(), event.hand(), stackName(event.item()));
        }
    }

    @Subscribe
    public void onEntityTame(EntityTameEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity tame: {} owner={}", event.entity().type().asString(), event.owner().name());
        }
    }

    @Subscribe
    public void onEntityBreed(EntityBreedEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity breed: {} + {} -> {} breeder={}",
                    event.parent().type().asString(),
                    event.partner().type().asString(),
                    event.child().type().asString(),
                    event.breeder().map(Player::name).orElse("none"));
        }
    }

    @Subscribe
    public void onEntityTransform(EntityTransformEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Entity transform: {} -> {} cause={}",
                    event.entity().type().asString(), event.targetType().asString(), event.cause());
        }
    }

    @Subscribe
    public void onHangingPlace(HangingPlaceEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Hanging place: {} face={} item={} player={}",
                    event.entity().type().asString(),
                    event.face(),
                    stackName(event.item()),
                    event.player().map(Player::name).orElse("none"));
        }
    }

    @Subscribe
    public void onHangingBreak(HangingBreakEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Hanging break: {} cause={} remover={}",
                    event.entity().type().asString(),
                    event.cause(),
                    event.remover().map(entity -> entity.type().asString()).orElse("none"));
        }
    }

    @Subscribe
    public void onItemFrameChange(PlayerItemFrameChangeEvent event) {
        if (context.config().getBoolean("features.log-entity-detail-events", false)) {
            logger.info("Item frame: {} action={} item={} rotation={}",
                    event.player().name(), event.action(), stackName(event.item()), event.rotation());
        }
    }

    @Subscribe
    public void onVehicleCreate(VehicleCreateEvent event) {
        if (context.config().getBoolean("features.log-vehicle-events", false)) {
            logger.info("Vehicle create: {} at {}", event.vehicle().type().asString(), compactLocation(event.vehicle().location()));
        }
    }

    @Subscribe
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (context.config().getBoolean("features.log-vehicle-events", false)) {
            logger.info("Vehicle destroy: {} attacker={}",
                    event.vehicle().type().asString(),
                    event.attacker().map(entity -> entity.type().asString()).orElse("none"));
        }
    }

    @Subscribe
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (context.config().getBoolean("features.log-vehicle-events", false)) {
            logger.info("Vehicle enter: {} <- {}", event.vehicle().type().asString(), event.entity().type().asString());
        }
    }

    @Subscribe
    public void onVehicleExit(VehicleExitEvent event) {
        if (context.config().getBoolean("features.log-vehicle-events", false)) {
            logger.info("Vehicle exit: {} -> {}", event.vehicle().type().asString(), event.entity().type().asString());
        }
    }

    @Subscribe
    public void onVehicleMove(VehicleMoveEvent event) {
        if (context.config().getBoolean("features.log-vehicle-move-events", false)) {
            logger.info("Vehicle move: {} {} -> {}",
                    event.vehicle().type().asString(), compactLocation(event.from()), compactLocation(event.to()));
        }
    }
}
