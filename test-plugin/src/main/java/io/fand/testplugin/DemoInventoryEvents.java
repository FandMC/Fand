package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.inventory.BrewEvent;
import io.fand.api.event.inventory.BrewingStandFuelEvent;
import io.fand.api.event.inventory.BlockCookEvent;
import io.fand.api.event.inventory.CraftItemEvent;
import io.fand.api.event.inventory.EnchantItemEvent;
import io.fand.api.event.inventory.FurnaceBurnEvent;
import io.fand.api.event.inventory.FurnaceExtractEvent;
import io.fand.api.event.inventory.FurnaceStartSmeltEvent;
import io.fand.api.event.inventory.FurnaceSmeltEvent;
import io.fand.api.event.inventory.HopperMoveItemEvent;
import io.fand.api.event.inventory.HopperPickupItemEvent;
import io.fand.api.event.inventory.InventoryClickEvent;
import io.fand.api.event.inventory.InventoryCloseEvent;
import io.fand.api.event.inventory.InventoryCreativeEvent;
import io.fand.api.event.inventory.InventoryDragEvent;
import io.fand.api.event.inventory.InventoryOpenEvent;
import io.fand.api.event.inventory.InventoryMoveItemEvent;
import io.fand.api.event.inventory.InventoryPickupItemEvent;
import io.fand.api.event.inventory.InventoryTradeEvent;
import io.fand.api.event.inventory.PrepareAnvilEvent;
import io.fand.api.event.inventory.PrepareItemEnchantEvent;
import io.fand.api.event.inventory.PrepareItemCraftEvent;
import io.fand.api.event.inventory.PrepareSmithingEvent;
import io.fand.api.event.inventory.PrepareTradeEvent;
import io.fand.api.inventory.Inventory;
import io.fand.api.plugin.PluginContext;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

final class DemoInventoryEvents implements Listener {

    private final PluginContext context;
    private final Logger logger;
    private final Set<UUID> demoGuiViewers;

    DemoInventoryEvents(PluginContext context, Set<UUID> demoGuiViewers) {
        this.context = context;
        this.logger = context.logger();
        this.demoGuiViewers = demoGuiViewers;
    }

    @Subscribe
    public void onInventoryOpen(InventoryOpenEvent event) {
        logger.debug("{} opened {}", event.player().name(), event.type());
    }

    @Subscribe
    public void onInventoryClose(InventoryCloseEvent event) {
        demoGuiViewers.remove(event.player().uniqueId());
        logger.debug("{} closed {}", event.player().name(), event.type());
    }

    @Subscribe
    public void onInventoryClick(InventoryClickEvent event) {
        if (isLockedDemoGuiClick(
                demoGuiViewers.contains(event.player().uniqueId()),
                event.inventory().type(),
                event.slot(),
                event.currentItem())) {
            event.setCancelled(true);
            event.player().sendMessage(Component.text(
                    message(context.config(), "messages.gui-locked", "This barrier is locked in the demo GUI."),
                    NamedTextColor.RED));
        }
        if (context.config().getBoolean("features.log-inventory-clicks", false)) {
            logger.info("{} clicked {} action={} slot={} current={} cursor={}",
                    event.player().name(), event.clickType(), event.action(), event.slot(),
                    stackName(event.currentItem()), stackName(event.cursorItem()));
        }
    }

    @Subscribe
    public void onInventoryDrag(InventoryDragEvent event) {
        if (context.config().getBoolean("features.log-inventory-clicks", false)) {
            logger.info("{} dragged {} across {} slots cursor={}",
                    event.player().name(), event.dragType(), event.slots().size(), stackName(event.cursorItem()));
        }
    }

    @Subscribe
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (context.config().getBoolean("features.log-inventory-clicks", false)) {
            logger.info("{} creative slot={} drop={} item={}",
                    event.player().name(), event.rawSlot(), event.drop(), stackName(event.item()));
        }
    }

    @Subscribe
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (context.config().getBoolean("features.log-inventory-moves", false)) {
            logger.info("Inventory moved {} from {} to {} sourceInitiated={}",
                    stackName(event.item()), event.source().type(), event.destination().type(), event.sourceInitiated());
        }
    }

    @Subscribe
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (context.config().getBoolean("features.log-inventory-moves", false)) {
            logger.info("Inventory picked up {} into {}", stackName(event.item()), event.inventory().type());
        }
    }

    @Subscribe
    public void onHopperMoveItem(HopperMoveItemEvent event) {
        if (context.config().getBoolean("features.log-inventory-moves", false)) {
            logger.info("Hopper moved {} at {} from {} to {} hopperInitiated={}",
                    stackName(event.item()),
                    compactLocation(event.hopperLocation()),
                    event.source().type(),
                    event.destination().type(),
                    event.hopperInitiated());
        }
    }

    @Subscribe
    public void onHopperPickupItem(HopperPickupItemEvent event) {
        if (context.config().getBoolean("features.log-inventory-moves", false)) {
            logger.info("Hopper picked up {} at {} itemEntity={}",
                    stackName(event.item()),
                    compactLocation(event.hopperLocation()),
                    event.itemEntity().type().key().asString());
        }
    }

    @Subscribe
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (context.config().getBoolean("features.log-crafting-events", false)) {
            logger.info("{} prepared craft recipe={} result={}",
                    event.player().name(),
                    event.recipe().map(recipe -> recipe.key().asString()).orElse("none"),
                    stackName(event.result()));
        }
    }

    @Subscribe
    public void onCraft(CraftItemEvent event) {
        if (context.config().getBoolean("features.log-crafting-events", false)) {
            logger.info("{} crafted recipe={} result={} click={}",
                    event.player().name(),
                    event.recipe().map(recipe -> recipe.key().asString()).orElse("none"),
                    stackName(event.result()),
                    event.clickType());
        }
    }

    @Subscribe
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("{} prepared enchant item={} shelves={} offers={}",
                    event.player().name(), stackName(event.item()), event.bookshelfPower(), event.offers().size());
        }
    }

    @Subscribe
    public void onEnchantItem(EnchantItemEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("{} enchanted item={} result={} xpCost={} enchantments={}",
                    event.player().name(),
                    stackName(event.inputItem()),
                    stackName(event.resultItem()),
                    event.xpCost(),
                    event.enchantments().size());
        }
    }

    @Subscribe
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("{} prepared anvil first={} second={} result={} cost={} rename={}",
                    event.player().name(),
                    stackName(event.firstItem()),
                    stackName(event.secondItem()),
                    stackName(event.result()),
                    event.cost(),
                    event.renameText().orElse(""));
        }
    }

    @Subscribe
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("{} prepared smithing recipe={} result={}",
                    event.player().name(),
                    event.recipe().map(recipe -> recipe.key().asString()).orElse("none"),
                    stackName(event.result()));
        }
    }

    @Subscribe
    public void onPrepareTrade(PrepareTradeEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("{} prepared trade {} + {} -> {} xp={}",
                    event.player().name(),
                    stackName(event.firstCost()),
                    stackName(event.secondCost()),
                    stackName(event.result()),
                    event.villagerExperience());
        }
    }

    @Subscribe
    public void onTrade(InventoryTradeEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("{} traded {} + {} -> {} xp={}",
                    event.player().name(),
                    stackName(event.firstCost()),
                    stackName(event.secondCost()),
                    stackName(event.result()),
                    event.villagerExperience());
        }
    }

    @Subscribe
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("Furnace burn: {},{},{} fuel={} burnTime={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    stackName(event.fuel()), event.burnTime());
        }
    }

    @Subscribe
    public void onFurnaceStartSmelt(FurnaceStartSmeltEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("Furnace start smelt: {},{},{} source={} recipe={} totalCookTime={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    stackName(event.source()),
                    event.recipe().map(recipe -> recipe.key().asString()).orElse("none"),
                    event.totalCookTime());
        }
    }

    @Subscribe
    public void onBlockCook(BlockCookEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("Block cook: {},{},{} source={} result={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    stackName(event.source()),
                    stackName(event.result()));
        }
    }

    @Subscribe
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("Furnace smelt: {},{},{} source={} result={} recipe={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    stackName(event.source()),
                    stackName(event.result()),
                    event.recipe().map(recipe -> recipe.key().asString()).orElse("none"));
        }
    }

    @Subscribe
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("{} extracted {} x{} from furnace",
                    event.player().name(), stackName(event.item()), event.amount());
        }
    }

    @Subscribe
    public void onBrewingStandFuel(BrewingStandFuelEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("Brewing fuel: {},{},{} fuel={} power={} consume={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    stackName(event.fuel()), event.fuelPower(), event.consumeAmount());
        }
    }

    @Subscribe
    public void onBrew(BrewEvent event) {
        if (context.config().getBoolean("features.log-workstation-events", false)) {
            logger.info("Brewing stand: {},{},{} ingredient={} results={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    stackName(event.ingredient()), event.results().size());
        }
    }
}
