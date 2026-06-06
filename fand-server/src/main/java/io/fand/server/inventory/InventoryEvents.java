package io.fand.server.inventory;

import io.fand.api.event.inventory.ClickType;
import io.fand.api.event.inventory.DragType;
import io.fand.api.event.inventory.InventoryClickEvent;
import io.fand.api.event.inventory.InventoryCloseEvent;
import io.fand.api.event.inventory.InventoryDragEvent;
import io.fand.api.event.inventory.InventoryOpenEvent;
import io.fand.api.inventory.InventoryType;
import io.fand.api.item.ItemStack;
import io.fand.server.entity.FandPlayer;
import io.fand.server.hooks.FandHooks;
import io.fand.server.item.FandItemStacks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;

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
        if (normalisedSlot >= 0 && normalisedSlot < menu.slots.size()) {
            currentItem = FandItemStacks.fromVanilla(menu.slots.get(normalisedSlot).getItem());
        }
        ItemStack cursorItem = FandItemStacks.fromVanilla(menu.getCarried());
        var event = new InventoryClickEvent(
                fandPlayer,
                new ContainerMenuView(menu),
                normalisedSlot,
                clickType,
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
}
