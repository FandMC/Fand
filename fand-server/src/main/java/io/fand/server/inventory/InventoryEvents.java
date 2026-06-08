package io.fand.server.inventory;

import io.fand.api.event.inventory.BrewEvent;
import io.fand.api.event.inventory.BrewingStandFuelEvent;
import io.fand.api.event.inventory.ClickType;
import io.fand.api.event.inventory.DragType;
import io.fand.api.event.inventory.EnchantItemEvent;
import io.fand.api.event.inventory.EnchantmentOffer;
import io.fand.api.event.inventory.FurnaceBurnEvent;
import io.fand.api.event.inventory.FurnaceExtractEvent;
import io.fand.api.event.inventory.FurnaceSmeltEvent;
import io.fand.api.event.inventory.HopperMoveItemEvent;
import io.fand.api.event.inventory.HopperPickupItemEvent;
import io.fand.api.event.inventory.InventoryClickEvent;
import io.fand.api.event.inventory.InventoryCloseEvent;
import io.fand.api.event.inventory.InventoryCreativeEvent;
import io.fand.api.event.inventory.InventoryDragEvent;
import io.fand.api.event.inventory.InventoryTradeEvent;
import io.fand.api.event.inventory.InventoryMoveItemEvent;
import io.fand.api.event.inventory.InventoryOpenEvent;
import io.fand.api.event.inventory.InventoryPickupItemEvent;
import io.fand.api.event.inventory.PrepareAnvilEvent;
import io.fand.api.event.inventory.PrepareItemEnchantEvent;
import io.fand.api.event.inventory.PrepareTradeEvent;
import io.fand.api.event.inventory.PrepareSmithingEvent;
import io.fand.api.inventory.InventoryType;
import io.fand.api.item.ItemStack;
import io.fand.api.world.Location;
import io.fand.server.entity.FandPlayer;
import io.fand.server.block.FandBlock;
import io.fand.server.hooks.FandHooks;
import io.fand.server.item.FandItemStacks;
import io.fand.server.recipe.FandRecipes;
import io.fand.server.world.FandWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import org.jspecify.annotations.Nullable;

/**
 * Fires the public inventory events from vanilla call sites. Each method:
 *
 * <ul>
 *   <li>fast-paths out when no listeners are registered;</li>
 *   <li>resolves the {@link FandPlayer} handle (returns the don't-cancel default
 *       if the player has no handle yet, e.g. mid-login);</li>
 *   <li>swallows and logs listener exceptions — vanilla call sites must not
 *       see them.</li>
 * </ul>
 */
public final class InventoryEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryEvents.class);

    private InventoryEvents() {
    }

    /** Returns {@code true} if the open should proceed. */
    public static boolean fireOpen(ServerPlayer player, MenuType<?> type) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(InventoryOpenEvent.class)) {
            return true;
        }
        var fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        var event = new InventoryOpenEvent(fandPlayer, InventoryTypes.resolve(type));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("InventoryOpenEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static void fireClose(ServerPlayer player, AbstractContainerMenu menu) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(InventoryCloseEvent.class)) {
            return;
        }
        var fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return;
        }
        var event = new InventoryCloseEvent(fandPlayer, InventoryTypes.resolve(menu));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("InventoryCloseEvent listener failed", failure);
        }
    }

    /** Returns {@code true} if the click should proceed (not cancelled). */
    public static boolean fireClick(
            ServerPlayer player,
            AbstractContainerMenu menu,
            int slot,
            int button,
            ContainerInput input) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(InventoryClickEvent.class)) {
            return true;
        }
        var fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        ClickType clickType = ClickTypes.resolve(input, button, slot);
        int normalisedSlot = slot == ClickTypes.OUTSIDE_SLOT ? InventoryClickEvent.OUTSIDE : slot;
        ItemStack currentItem = ItemStack.EMPTY;
        int containerSlot = -1;
        int playerInventorySlot = -1;
        if (normalisedSlot >= 0 && normalisedSlot < menu.slots.size()) {
            var clickedSlot = menu.slots.get(normalisedSlot);
            currentItem = FandItemStacks.fromVanilla(clickedSlot.getItem());
            if (clickedSlot.container == player.getInventory()) {
                playerInventorySlot = clickedSlot.getContainerSlot();
            } else {
                containerSlot = clickedSlot.getContainerSlot();
            }
        }
        ItemStack cursorItem = FandItemStacks.fromVanilla(menu.getCarried());
        var action = ClickTypes.action(clickType, slot, currentItem, cursorItem);
        var event = new InventoryClickEvent(
                fandPlayer,
                new ContainerMenuView(menu),
                normalisedSlot,
                slot,
                containerSlot,
                playerInventorySlot,
                clickType,
                action,
                button,
                currentItem,
                cursorItem);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("InventoryClickEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static ClickType resolveClickType(ContainerInput input, int button, int slot) {
        return ClickTypes.resolve(input, button, slot);
    }

    public static MoveItemResult fireMoveItem(
            Container source,
            Container destination,
            net.minecraft.world.item.ItemStack itemStack,
            boolean sourceInitiated
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(InventoryMoveItemEvent.class)) {
            return new MoveItemResult(true, itemStack);
        }
        var event = new InventoryMoveItemEvent(
                new FandContainerInventory(source, InventoryType.UNKNOWN),
                new FandContainerInventory(destination, InventoryType.UNKNOWN),
                FandItemStacks.fromVanilla(itemStack),
                sourceInitiated);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("InventoryMoveItemEvent listener failed", failure);
            return new MoveItemResult(true, itemStack);
        }
        if (event.cancelled() || event.item().isEmpty()) {
            return new MoveItemResult(false, itemStack);
        }
        try {
            return new MoveItemResult(true, FandItemStacks.toVanilla(event.item()));
        } catch (RuntimeException failure) {
            LOGGER.warn("InventoryMoveItemEvent supplied an invalid item stack", failure);
            return new MoveItemResult(true, itemStack);
        }
    }

    public record MoveItemResult(boolean allowed, net.minecraft.world.item.ItemStack itemStack) {
    }

    public static MoveItemResult fireHopperMoveItem(
            Container source,
            Container destination,
            net.minecraft.world.item.ItemStack itemStack
    ) {
        var bus = FandHooks.events();
        boolean sourceIsHopper = source instanceof Hopper;
        boolean destinationIsHopper = destination instanceof Hopper;
        if ((!sourceIsHopper && !destinationIsHopper) || !bus.hasListeners(HopperMoveItemEvent.class)) {
            return new MoveItemResult(true, itemStack);
        }
        var hopperContainer = sourceIsHopper ? source : destination;
        var hopper = (Hopper) hopperContainer;
        var world = hopperWorld(hopperContainer);
        if (world == null) {
            return new MoveItemResult(true, itemStack);
        }
        var event = new HopperMoveItemEvent(
                new FandContainerInventory(hopperContainer, InventoryType.HOPPER),
                new FandContainerInventory(source, InventoryType.UNKNOWN),
                new FandContainerInventory(destination, InventoryType.UNKNOWN),
                hopperLocation(world, hopper),
                FandItemStacks.fromVanilla(itemStack),
                sourceIsHopper);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("HopperMoveItemEvent listener failed", failure);
            return new MoveItemResult(true, itemStack);
        }
        if (event.cancelled() || event.item().isEmpty()) {
            return new MoveItemResult(false, itemStack);
        }
        try {
            return new MoveItemResult(true, FandItemStacks.toVanilla(event.item()));
        } catch (RuntimeException failure) {
            LOGGER.warn("HopperMoveItemEvent supplied an invalid item stack", failure);
            return new MoveItemResult(true, itemStack);
        }
    }

    public static MoveItemResult firePickupItem(
            Container inventory,
            net.minecraft.world.item.ItemStack itemStack
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(InventoryPickupItemEvent.class)) {
            return new MoveItemResult(true, itemStack);
        }
        var event = new InventoryPickupItemEvent(
                new FandContainerInventory(inventory, InventoryType.UNKNOWN),
                FandItemStacks.fromVanilla(itemStack));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("InventoryPickupItemEvent listener failed", failure);
            return new MoveItemResult(true, itemStack);
        }
        if (event.cancelled() || event.item().isEmpty()) {
            return new MoveItemResult(false, itemStack);
        }
        try {
            return new MoveItemResult(true, FandItemStacks.toVanilla(event.item()));
        } catch (RuntimeException failure) {
            LOGGER.warn("InventoryPickupItemEvent supplied an invalid item stack", failure);
            return new MoveItemResult(true, itemStack);
        }
    }

    public static MoveItemResult fireHopperPickupItem(
            Container inventory,
            ItemEntity itemEntity,
            net.minecraft.world.item.ItemStack itemStack
    ) {
        var bus = FandHooks.events();
        if (!(inventory instanceof Hopper hopper) || !bus.hasListeners(HopperPickupItemEvent.class)) {
            return new MoveItemResult(true, itemStack);
        }
        var world = hopperWorld(inventory);
        var fandEntity = FandHooks.wrapEntity(itemEntity);
        if (world == null || fandEntity == null) {
            return new MoveItemResult(true, itemStack);
        }
        var event = new HopperPickupItemEvent(
                new FandContainerInventory(inventory, InventoryType.HOPPER),
                hopperLocation(world, hopper),
                fandEntity,
                FandItemStacks.fromVanilla(itemStack));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("HopperPickupItemEvent listener failed", failure);
            return new MoveItemResult(true, itemStack);
        }
        if (event.cancelled() || event.item().isEmpty()) {
            return new MoveItemResult(false, itemStack);
        }
        try {
            return new MoveItemResult(true, FandItemStacks.toVanilla(event.item()));
        } catch (RuntimeException failure) {
            LOGGER.warn("HopperPickupItemEvent supplied an invalid item stack", failure);
            return new MoveItemResult(true, itemStack);
        }
    }

    public static net.minecraft.world.item.@Nullable ItemStack fireCreative(
            ServerPlayer player,
            int rawSlot,
            boolean drop,
            net.minecraft.world.item.ItemStack itemStack
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(InventoryCreativeEvent.class)) {
            return itemStack;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return itemStack;
        }
        var event = new InventoryCreativeEvent(fandPlayer, rawSlot, drop, FandItemStacks.fromVanilla(itemStack));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("InventoryCreativeEvent listener failed", failure);
            return itemStack;
        }
        if (event.cancelled()) {
            return null;
        }
        try {
            return FandItemStacks.toVanilla(event.item());
        } catch (RuntimeException failure) {
            LOGGER.warn("InventoryCreativeEvent supplied an invalid item stack", failure);
            return itemStack;
        }
    }

    /**
     * Returns {@code true} if the drag placement should proceed. Called from
     * {@link AbstractContainerMenu#doClick} at QUICKCRAFT_HEADER_END after the
     * dragged slots are collected and validated, but before the placement
     * loop runs.
     */
    public static boolean fireDrag(
            ServerPlayer player,
            AbstractContainerMenu menu,
            int quickcraftType,
            java.util.Set<Integer> slotIndices) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(InventoryDragEvent.class)) {
            return true;
        }
        var fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        DragType dragType = switch (quickcraftType) {
            case AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE -> DragType.EVEN;
            case AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY -> DragType.SINGLE;
            case AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE -> DragType.CLONE;
            default -> DragType.EVEN;
        };
        ItemStack cursorItem = FandItemStacks.fromVanilla(menu.getCarried());
        var event = new InventoryDragEvent(
                fandPlayer,
                new ContainerMenuView(menu),
                dragType,
                slotIndices,
                cursorItem);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("InventoryDragEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static void firePrepareEnchant(
            ServerPlayer player,
            AbstractContainerMenu menu,
            RegistryAccess access,
            net.minecraft.world.item.ItemStack itemStack,
            int bookshelfPower,
            int[] costs,
            int[] enchantClue,
            int[] levelClue
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PrepareItemEnchantEvent.class)) {
            return;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return;
        }
        var offers = new ArrayList<EnchantmentOffer>();
        var holders = access.lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
        for (int slot = 0; slot < costs.length; slot++) {
            Optional<Key> enchantment = Optional.ofNullable(holders.byId(enchantClue[slot]))
                    .flatMap(Holder::unwrapKey)
                    .map(ResourceKey::identifier)
                    .map(InventoryEvents::key);
            offers.add(new EnchantmentOffer(slot, costs[slot], enchantment, levelClue[slot]));
        }
        var event = new PrepareItemEnchantEvent(
                fandPlayer,
                new ContainerMenuView(menu),
                FandItemStacks.fromVanilla(itemStack),
                bookshelfPower,
                offers);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PrepareItemEnchantEvent listener failed", failure);
            return;
        }
        for (var offer : event.offers()) {
            int slot = offer.slot();
            if (slot < 0 || slot >= costs.length) {
                continue;
            }
            costs[slot] = offer.cost();
            levelClue[slot] = offer.level();
            enchantClue[slot] = offer.enchantment()
                    .flatMap(enchantment -> resolveEnchantmentId(access, enchantment))
                    .orElse(-1);
        }
    }

    public static net.minecraft.world.item.@Nullable ItemStack fireEnchantItem(
            ServerPlayer player,
            AbstractContainerMenu menu,
            net.minecraft.world.item.ItemStack inputItem,
            net.minecraft.world.item.ItemStack resultItem,
            int button,
            int levelCost,
            int xpCost,
            List<EnchantmentInstance> enchantments
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(EnchantItemEvent.class)) {
            return resultItem;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return resultItem;
        }
        var offers = enchantments.stream()
                .map(enchantment -> new EnchantmentOffer(
                        button,
                        xpCost,
                        enchantment.enchantment().unwrapKey().map(ResourceKey::identifier).map(InventoryEvents::key),
                        enchantment.level()))
                .toList();
        var event = new EnchantItemEvent(
                fandPlayer,
                new ContainerMenuView(menu),
                FandItemStacks.fromVanilla(inputItem),
                FandItemStacks.fromVanilla(resultItem),
                button,
                levelCost,
                xpCost,
                offers);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("EnchantItemEvent listener failed", failure);
            return resultItem;
        }
        if (event.cancelled() || event.resultItem().isEmpty()) {
            return null;
        }
        try {
            return FandItemStacks.toVanilla(event.resultItem());
        } catch (RuntimeException failure) {
            LOGGER.warn("EnchantItemEvent supplied an invalid result item", failure);
            return resultItem;
        }
    }

    public static AnvilResult firePrepareAnvil(
            ServerPlayer player,
            AbstractContainerMenu menu,
            net.minecraft.world.item.ItemStack firstItem,
            net.minecraft.world.item.ItemStack secondItem,
            net.minecraft.world.item.ItemStack result,
            int cost,
            Optional<String> renameText
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PrepareAnvilEvent.class)) {
            return new AnvilResult(result, cost);
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return new AnvilResult(result, cost);
        }
        var event = new PrepareAnvilEvent(
                fandPlayer,
                new ContainerMenuView(menu),
                FandItemStacks.fromVanilla(firstItem),
                FandItemStacks.fromVanilla(secondItem),
                FandItemStacks.fromVanilla(result),
                cost,
                renameText);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PrepareAnvilEvent listener failed", failure);
            return new AnvilResult(result, cost);
        }
        try {
            return new AnvilResult(FandItemStacks.toVanilla(event.result()), event.cost());
        } catch (RuntimeException failure) {
            LOGGER.warn("PrepareAnvilEvent supplied an invalid result item", failure);
            return new AnvilResult(result, cost);
        }
    }

    public static net.minecraft.world.item.ItemStack firePrepareSmithing(
            ServerPlayer player,
            AbstractContainerMenu menu,
            Optional<RecipeHolder<?>> recipe,
            net.minecraft.world.item.ItemStack templateItem,
            net.minecraft.world.item.ItemStack baseItem,
            net.minecraft.world.item.ItemStack additionItem,
            net.minecraft.world.item.ItemStack result
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PrepareSmithingEvent.class)) {
            return result;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return result;
        }
        var event = new PrepareSmithingEvent(
                fandPlayer,
                new ContainerMenuView(menu),
                recipe.map(FandRecipes::fromVanilla),
                FandItemStacks.fromVanilla(templateItem),
                FandItemStacks.fromVanilla(baseItem),
                FandItemStacks.fromVanilla(additionItem),
                FandItemStacks.fromVanilla(result));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PrepareSmithingEvent listener failed", failure);
            return result;
        }
        try {
            return FandItemStacks.toVanilla(event.result());
        } catch (RuntimeException failure) {
            LOGGER.warn("PrepareSmithingEvent supplied an invalid result item", failure);
            return result;
        }
    }

    public static net.minecraft.world.item.ItemStack firePrepareTrade(
            ServerPlayer player,
            Container inventory,
            net.minecraft.world.item.ItemStack firstCost,
            net.minecraft.world.item.ItemStack secondCost,
            net.minecraft.world.item.ItemStack result,
            int villagerExperience
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PrepareTradeEvent.class)) {
            return result;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return result;
        }
        var event = new PrepareTradeEvent(
                fandPlayer,
                new FandContainerInventory(inventory, InventoryType.MERCHANT),
                FandItemStacks.fromVanilla(firstCost),
                FandItemStacks.fromVanilla(secondCost),
                FandItemStacks.fromVanilla(result),
                villagerExperience);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PrepareTradeEvent listener failed", failure);
            return result;
        }
        try {
            return FandItemStacks.toVanilla(event.result());
        } catch (RuntimeException failure) {
            LOGGER.warn("PrepareTradeEvent supplied an invalid result item", failure);
            return result;
        }
    }

    public static boolean fireTrade(
            ServerPlayer player,
            Container inventory,
            MerchantOffer offer,
            net.minecraft.world.item.ItemStack firstCost,
            net.minecraft.world.item.ItemStack secondCost,
            net.minecraft.world.item.ItemStack result
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(InventoryTradeEvent.class)) {
            return true;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return true;
        }
        var event = new InventoryTradeEvent(
                fandPlayer,
                new FandContainerInventory(inventory, InventoryType.MERCHANT),
                FandItemStacks.fromVanilla(firstCost),
                FandItemStacks.fromVanilla(secondCost),
                FandItemStacks.fromVanilla(result),
                offer.getXp());
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("InventoryTradeEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static int fireFurnaceBurn(
            ServerLevel level,
            net.minecraft.core.BlockPos pos,
            Container inventory,
            net.minecraft.world.item.ItemStack fuel,
            int burnTime
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(FurnaceBurnEvent.class)) {
            return burnTime;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return burnTime;
        }
        var event = new FurnaceBurnEvent(
                new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()),
                new FandContainerInventory(inventory, InventoryType.FURNACE),
                FandItemStacks.fromVanilla(fuel),
                burnTime);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("FurnaceBurnEvent listener failed", failure);
            return burnTime;
        }
        return event.cancelled() ? 0 : event.burnTime();
    }

    public static net.minecraft.world.item.@Nullable ItemStack fireFurnaceSmelt(
            ServerLevel level,
            net.minecraft.core.BlockPos pos,
            Container inventory,
            Optional<RecipeHolder<?>> recipe,
            net.minecraft.world.item.ItemStack source,
            net.minecraft.world.item.ItemStack result
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(FurnaceSmeltEvent.class)) {
            return result;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return result;
        }
        var event = new FurnaceSmeltEvent(
                new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()),
                new FandContainerInventory(inventory, InventoryType.FURNACE),
                recipe.map(FandRecipes::fromVanilla),
                FandItemStacks.fromVanilla(source),
                FandItemStacks.fromVanilla(result));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("FurnaceSmeltEvent listener failed", failure);
            return result;
        }
        if (event.cancelled() || event.result().isEmpty()) {
            return null;
        }
        try {
            return FandItemStacks.toVanilla(event.result());
        } catch (RuntimeException failure) {
            LOGGER.warn("FurnaceSmeltEvent supplied an invalid result item", failure);
            return result;
        }
    }

    public static void fireFurnaceExtract(
            ServerPlayer player,
            Container inventory,
            net.minecraft.world.item.ItemStack item,
            int amount
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(FurnaceExtractEvent.class) || amount <= 0) {
            return;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return;
        }
        try {
            bus.fire(new FurnaceExtractEvent(
                    fandPlayer,
                    new FandContainerInventory(inventory, InventoryType.FURNACE),
                    FandItemStacks.fromVanilla(item),
                    amount));
        } catch (RuntimeException failure) {
            LOGGER.warn("FurnaceExtractEvent listener failed", failure);
        }
    }

    public static @Nullable List<net.minecraft.world.item.ItemStack> fireBrew(
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            Container inventory,
            net.minecraft.world.item.ItemStack ingredient,
            List<net.minecraft.world.item.ItemStack> results
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BrewEvent.class)) {
            return results;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return results;
        }
        var world = FandHooks.wrapWorld(serverLevel);
        if (world == null) {
            return results;
        }
        var event = new BrewEvent(
                new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()),
                new FandContainerInventory(inventory, InventoryType.BREWING),
                FandItemStacks.fromVanilla(ingredient),
                results.stream().map(FandItemStacks::fromVanilla).toList());
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BrewEvent listener failed", failure);
            return results;
        }
        if (event.cancelled()) {
            return null;
        }
        try {
            return event.results().stream().map(FandItemStacks::toVanilla).toList();
        } catch (RuntimeException failure) {
            LOGGER.warn("BrewEvent supplied invalid result items", failure);
            return results;
        }
    }

    public static @Nullable BrewingStandFuelResult fireBrewingStandFuel(
            Level level,
            net.minecraft.core.BlockPos pos,
            Container inventory,
            net.minecraft.world.item.ItemStack fuel,
            int fuelPower,
            int consumeAmount
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BrewingStandFuelEvent.class)) {
            return new BrewingStandFuelResult(fuelPower, consumeAmount);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return new BrewingStandFuelResult(fuelPower, consumeAmount);
        }
        var world = FandHooks.wrapWorld(serverLevel);
        if (world == null) {
            return new BrewingStandFuelResult(fuelPower, consumeAmount);
        }
        var event = new BrewingStandFuelEvent(
                new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()),
                new FandContainerInventory(inventory, InventoryType.BREWING),
                FandItemStacks.fromVanilla(fuel),
                fuelPower,
                consumeAmount);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BrewingStandFuelEvent listener failed", failure);
            return new BrewingStandFuelResult(fuelPower, consumeAmount);
        }
        return event.cancelled() ? null : new BrewingStandFuelResult(event.fuelPower(), event.consumeAmount());
    }

    public record AnvilResult(net.minecraft.world.item.ItemStack itemStack, int cost) {
    }

    public record BrewingStandFuelResult(int fuelPower, int consumeAmount) {
    }

    private static @Nullable FandWorld hopperWorld(Container hopperContainer) {
        Level level = null;
        if (hopperContainer instanceof BlockEntity blockEntity) {
            level = blockEntity.getLevel();
        } else if (hopperContainer instanceof net.minecraft.world.entity.Entity entity) {
            level = entity.level();
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return FandHooks.wrapWorld(serverLevel);
    }

    private static Location hopperLocation(FandWorld world, Hopper hopper) {
        return new Location(world, hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ(), 0.0F, 0.0F);
    }

    private static Optional<Integer> resolveEnchantmentId(RegistryAccess access, Key key) {
        var registry = access.lookupOrThrow(Registries.ENCHANTMENT);
        var holder = registry.get(ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(key.namespace(), key.value())));
        return holder.map(value -> registry.asHolderIdMap().getId((Holder<Enchantment>) value));
    }

    private static Key key(Identifier identifier) {
        return Key.key(identifier.getNamespace(), identifier.getPath());
    }
}
