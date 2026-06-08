package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.entity.Player;
import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.entity.EntityDamageEvent;
import io.fand.api.event.player.PlayerAdvancementDoneEvent;
import io.fand.api.event.player.PlayerArmorStandManipulateEvent;
import io.fand.api.event.player.PlayerBedEnterEvent;
import io.fand.api.event.player.PlayerBedLeaveEvent;
import io.fand.api.event.player.PlayerBucketEmptyEvent;
import io.fand.api.event.player.PlayerBucketFillEvent;
import io.fand.api.event.player.PlayerChangedMainHandEvent;
import io.fand.api.event.player.PlayerChangedWorldEvent;
import io.fand.api.event.player.PlayerClientBrandEvent;
import io.fand.api.event.player.PlayerDeathEvent;
import io.fand.api.event.player.PlayerDropItemEvent;
import io.fand.api.event.player.PlayerEditBookEvent;
import io.fand.api.event.player.PlayerEggThrowEvent;
import io.fand.api.event.player.PlayerExperienceChangeEvent;
import io.fand.api.event.player.PlayerFoodLevelChangeEvent;
import io.fand.api.event.player.PlayerGameModeChangeEvent;
import io.fand.api.event.player.PlayerInteractEntityEvent;
import io.fand.api.event.player.PlayerInteractEvent;
import io.fand.api.event.player.PlayerFishEvent;
import io.fand.api.event.player.PlayerItemConsumeEvent;
import io.fand.api.event.player.PlayerItemDamageEvent;
import io.fand.api.event.player.PlayerItemHeldEvent;
import io.fand.api.event.player.PlayerKickEvent;
import io.fand.api.event.player.PlayerLeashEntityEvent;
import io.fand.api.event.player.PlayerLevelChangeEvent;
import io.fand.api.event.player.PlayerLocaleChangeEvent;
import io.fand.api.event.player.PlayerMoveEvent;
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
import io.fand.api.plugin.PluginContext;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

final class DemoPlayerEvents implements Listener {

    private final PluginContext context;
    private final Logger logger;
    private final Set<UUID> demoGuiViewers;

    DemoPlayerEvents(PluginContext context, Set<UUID> demoGuiViewers) {
        this.context = context;
        this.logger = context.logger();
        this.demoGuiViewers = demoGuiViewers;
    }

    @Subscribe
    public void onDrop(PlayerDropItemEvent event) {
        if (isKitNavigator(event.item())) {
            event.setCancelled(true);
            event.player().sendMessage(Component.text("The kit navigator stays with you.", NamedTextColor.YELLOW));
            return;
        }
        if (context.config().getBoolean("features.log-item-events", false)) {
            logger.info("{} dropped {} hand={}", event.player().name(), stackName(event.item()), event.thrownFromHand());
        }
    }

    @Subscribe
    public void onPickup(PlayerPickupItemEvent event) {
        if (context.config().getBoolean("features.log-item-events", false)) {
            logger.info("{} picked up {}", event.player().name(), stackName(event.item()));
        }
    }

    @Subscribe
    public void onFish(PlayerFishEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} fishing state={} caught={} drops={}",
                    event.player().name(),
                    event.state(),
                    event.caught().map(entity -> entity.type().asString()).orElse("none"),
                    event.drops().size());
        }
    }

    @Subscribe
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (event.item().customData()
                .map(data -> data.has("demo_role") && data.get("demo_role").getAsString().equals("fand_kit_snack"))
                .orElse(false)) {
            event.player().sendActionBar(Component.text("Snack consume event observed.", NamedTextColor.GOLD));
        }
    }

    @Subscribe
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (context.config().getBoolean("features.log-item-events", false)) {
            logger.info("{} item damage {} +{}", event.player().name(), stackName(event.item()), event.damage());
        }
    }

    @Subscribe
    public void onFoodLevelChange(PlayerFoodLevelChangeEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} food {} -> {} saturation {} -> {} cause={}",
                    event.player().name(), event.fromLevel(), event.toLevel(),
                    trim(event.fromSaturation()), trim(event.toSaturation()), event.cause());
        }
    }

    @Subscribe
    public void onExperienceChange(PlayerExperienceChangeEvent event) {
        if (event.amount() > 0) {
            event.player().sendActionBar(Component.text("XP +" + event.amount() + " observed by Fand.", NamedTextColor.GREEN));
        }
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} xp delta={}", event.player().name(), event.amount());
        }
    }

    @Subscribe
    public void onLevelChange(PlayerLevelChangeEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} level {} -> {}", event.player().name(), event.oldLevel(), event.newLevel());
        }
    }

    @Subscribe
    public void onMove(PlayerMoveEvent event) {
        if (context.config().getBoolean("features.log-player-move-events", false)) {
            logger.info("{} moved {} -> {}",
                    event.player().name(), compactLocation(event.from()), compactLocation(event.to()));
        }
    }

    @Subscribe
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (isKitNavigator(event.offHandItem())) {
            event.setCancelled(true);
            event.player().sendMessage(Component.text("The kit navigator cannot be moved to off-hand.", NamedTextColor.YELLOW));
        }
    }

    @Subscribe
    public void onVelocity(PlayerVelocityEvent event) {
        double speedSquared = event.x() * event.x() + event.y() * event.y() + event.z() * event.z();
        if (context.config().getBoolean("protections.limit-player-velocity", true) && speedSquared > 100.0) {
            event.setVelocity(0.0, Math.min(event.y(), 1.0), 0.0);
            event.player().sendActionBar(Component.text("Extreme velocity was limited by the test plugin.", NamedTextColor.YELLOW));
        }
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} velocity {},{},{}", event.player().name(), trim(event.x()), trim(event.y()), trim(event.z()));
        }
    }

    @Subscribe
    public void onChangedMainHand(PlayerChangedMainHandEvent event) {
        if (context.config().getBoolean("features.log-player-state-events", false)) {
            logger.info("{} main hand {} -> {}", event.player().name(), event.oldMainHand(), event.newMainHand());
        }
    }

    @Subscribe
    public void onStatisticIncrement(PlayerStatisticIncrementEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} stat {} {} -> {}",
                    event.player().name(), event.statistic().asString(), event.previousValue(), event.newValue());
        }
    }

    @Subscribe
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} discovered recipes {}", event.player().name(), event.recipes().size());
        }
    }

    @Subscribe
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (context.config().getBoolean("features.log-player-state-events", false)) {
            logger.info("{} sneaking={}", event.player().name(), event.sneaking());
        }
    }

    @Subscribe
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        if (context.config().getBoolean("features.log-player-state-events", false)) {
            logger.info("{} sprinting={}", event.player().name(), event.sprinting());
        }
    }

    @Subscribe
    public void onTeleport(PlayerTeleportEvent event) {
        if (context.config().getBoolean("protections.block-low-teleport", true)
                && event.to().y() < -64.0) {
            event.setCancelled(true);
            event.player().sendMessage(Component.text("Teleport below world min height was cancelled.", NamedTextColor.RED));
            return;
        }
        if (context.config().getBoolean("features.log-teleports", false)) {
            logger.info("{} teleport {} {} -> {}", event.player().name(), event.cause(),
                    compactLocation(event.from()), compactLocation(event.to()));
        }
    }

    @Subscribe
    public void onPortal(PlayerPortalEvent event) {
        if (context.config().getBoolean("features.log-teleports", false)) {
            logger.info("{} portal {} -> {}", event.player().name(), compactLocation(event.from()), compactLocation(event.to()));
        }
    }

    @Subscribe
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (context.config().getBoolean("features.log-teleports", false)) {
            logger.info("{} changed world {} -> {}",
                    event.player().name(), event.fromWorld().key().asString(), event.toWorld().key().asString());
        }
    }

    @Subscribe
    public void onRespawn(PlayerRespawnEvent event) {
        event.player().sendMessage(Component.text("Respawned by " + event.cause()
                + " at " + compactLocation(event.respawnLocation()), NamedTextColor.AQUA));
    }

    @Subscribe
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (context.config().getBoolean("features.decorate-player-death-message", true)
                && event.deathMessage() != null) {
            event.setDeathMessage(event.deathMessage().append(Component.text(" [Fand]", NamedTextColor.GRAY)));
        }
    }

    @Subscribe
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} entered bed at {},{},{}",
                    event.player().name(), event.bed().x(), event.bed().y(), event.bed().z());
        }
    }

    @Subscribe
    public void onBedLeave(PlayerBedLeaveEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} left bed at {},{},{} forceful={}",
                    event.player().name(), event.bed().x(), event.bed().y(), event.bed().z(), event.forcefulWakeUp());
        }
    }

    @Subscribe
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} completed advancement {}", event.player().name(), event.advancement().asString());
        }
    }

    @Subscribe
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (context.config().getBoolean("features.log-player-state-events", false)) {
            logger.info("{} game mode {} -> {}", event.player().name(), event.fromGameMode(), event.toGameMode());
        }
    }

    @Subscribe
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        if (context.config().getBoolean("features.log-player-state-events", false)) {
            logger.info("{} locale {} -> {}", event.player().name(), event.oldLocale(), event.newLocale());
        }
    }

    @Subscribe
    public void onClientBrand(PlayerClientBrandEvent event) {
        if (context.config().getBoolean("features.log-player-state-events", false)) {
            logger.info("{} client brand {}", event.player().name(), event.brand());
        }
    }

    @Subscribe
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (context.config().getBoolean("features.log-player-state-events", false)) {
            logger.info("{} resource pack {} {}", event.player().name(), event.id(), event.status());
        }
    }

    @Subscribe
    public void onKick(PlayerKickEvent event) {
        if (context.config().getBoolean("features.decorate-kick-reason", true)) {
            event.setReason(event.reason().append(Component.text(" (via Fand event)", NamedTextColor.GRAY)));
        }
    }

    @Subscribe
    public void onInteract(PlayerInteractEvent event) {
        if (event.hand() == PlayerInteractEvent.Hand.MAIN_HAND
                && isKitNavigator(event.item())) {
            event.setCancelled(true);
            openDemoInventory(
                    context,
                    event.player(),
                    event.player(),
                    demoGuiViewers,
                    demoKitInventory(context, event.player().name()),
                    message(context.config(), "messages.kit-navigator", "Opened your Fand kit navigator."));
            event.player().sendActionBar(Component.text("Fand kit navigator opened.", NamedTextColor.AQUA));
            return;
        }
        if (!context.config().getBoolean("features.report-right-clicks", false)
                || event.hand() != PlayerInteractEvent.Hand.MAIN_HAND) {
            return;
        }
        event.block().ifPresent(block -> event.player().sendMessage(Component.text(
                "Right-clicked " + blockName(block.type()) + " at " + block.x() + "," + block.y() + "," + block.z(),
                NamedTextColor.GRAY)));
    }

    @Subscribe
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)
                && event.hand() == PlayerInteractEvent.Hand.MAIN_HAND) {
            logger.info("{} interacted with {} item={} precise={}",
                    event.player().name(),
                    event.entity().type().asString(),
                    stackName(event.item()),
                    event.preciseInteraction());
        }
    }

    @Subscribe
    public void onShearEntity(PlayerShearEntityEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} sheared {} with {}",
                    event.player().name(),
                    event.entity().type().asString(),
                    stackName(event.tool()));
        }
    }

    @Subscribe
    public void onLeashEntity(PlayerLeashEntityEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} leashed {} to {} cause={}",
                    event.player().name(),
                    event.entity().type().asString(),
                    event.holder().map(entity -> entity.type().asString()).orElse("none"),
                    event.cause());
        }
    }

    @Subscribe
    public void onUnleashEntity(PlayerUnleashEntityEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} unleashed {} holder={} dropLead={}",
                    event.player().name(),
                    event.entity().type().asString(),
                    event.holder().map(entity -> entity.type().asString()).orElse("none"),
                    event.dropLead());
        }
    }

    @Subscribe
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} armor stand slot={} playerItem={} standItem={}",
                    event.player().name(),
                    event.slot(),
                    stackName(event.playerItem()),
                    stackName(event.armorStandItem()));
        }
    }

    @Subscribe
    public void onEditBook(PlayerEditBookEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} edited book slot={} signing={} title={} new={}",
                    event.player().name(),
                    event.slot(),
                    event.signing(),
                    event.title().orElse(""),
                    stackName(event.newBook()));
        }
    }

    @Subscribe
    public void onEggThrow(PlayerEggThrowEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} egg throw hatching={} count={}",
                    event.player().name(), event.hatching(), event.hatchCount());
        }
    }

    @Subscribe
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} hotbar {} -> {} item={}",
                    event.player().name(), event.previousSlot(), event.newSlot(), stackName(event.newItem()));
        }
    }

    @Subscribe
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} filled bucket at {},{},{} result={}",
                    event.player().name(), event.block().x(), event.block().y(), event.block().z(), stackName(event.resultItem()));
        }
    }

    @Subscribe
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (context.config().getBoolean("features.log-player-detail-events", false)) {
            logger.info("{} emptied bucket at {},{},{} fluid={} result={}",
                    event.player().name(), event.block().x(), event.block().y(), event.block().z(),
                    event.fluid().asString(), stackName(event.resultItem()));
        }
    }

    @Subscribe
    public void onDamage(EntityDamageEvent event) {
        if (context.config().getBoolean("protections.cancel-fall-damage", true)
                && event.entity() instanceof Player player
                && event.cause().equals("minecraft:fall")) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Fall damage cancelled by test-plugin.", NamedTextColor.YELLOW));
        }
    }
}
