package io.fand.server.inventory;

import io.fand.api.event.inventory.ClickType;
import io.fand.api.event.inventory.InventoryAction;
import io.fand.api.item.ItemStack;
import net.minecraft.world.inventory.ContainerInput;

/**
 * Maps the vanilla ({@link ContainerInput}, button, slot) tuple from a
 * container click packet onto the public {@link ClickType} enum.
 *
 * <p>The slot index is consulted because vanilla overloads
 * {@code PICKUP} with a {@code -999} sentinel for "outside any slot",
 * which means "drop the carried stack".
 */
final class ClickTypes {

    static final int OUTSIDE_SLOT = -999;

    private ClickTypes() {
    }

    static ClickType resolve(ContainerInput input, int button, int slot) {
        return switch (input) {
            case PICKUP -> {
                if (slot == OUTSIDE_SLOT) {
                    yield button == 0 ? ClickType.DROP_ALL : ClickType.DROP;
                }
                yield button == 0 ? ClickType.PICKUP : ClickType.PICKUP_HALF;
            }
            case QUICK_MOVE -> button == 0 ? ClickType.QUICK_MOVE : ClickType.QUICK_MOVE_ALL;
            case SWAP -> button == 40 ? ClickType.SWAP_OFFHAND : ClickType.SWAP;
            case CLONE -> ClickType.CLONE;
            case THROW -> button == 0 ? ClickType.DROP : ClickType.DROP_ALL;
            case QUICK_CRAFT -> ClickType.QUICK_CRAFT;
            case PICKUP_ALL -> ClickType.PICKUP_ALL;
        };
    }

    static InventoryAction action(ClickType clickType, int slot, ItemStack currentItem, ItemStack cursorItem) {
        boolean outside = slot == OUTSIDE_SLOT;
        return switch (clickType) {
            case PICKUP -> pickupAction(currentItem, cursorItem);
            case PICKUP_HALF -> pickupHalfAction(currentItem, cursorItem);
            case QUICK_MOVE, QUICK_MOVE_ALL -> currentItem.empty()
                    ? InventoryAction.NOTHING
                    : InventoryAction.MOVE_TO_OTHER_INVENTORY;
            case SWAP, SWAP_OFFHAND -> currentItem.empty()
                    ? InventoryAction.HOTBAR_MOVE_AND_READD
                    : InventoryAction.HOTBAR_SWAP;
            case CLONE -> currentItem.empty() ? InventoryAction.NOTHING : InventoryAction.CLONE_STACK;
            case DROP -> dropAction(outside, currentItem, cursorItem, false);
            case DROP_ALL -> dropAction(outside, currentItem, cursorItem, true);
            case PICKUP_ALL -> cursorItem.empty() ? InventoryAction.NOTHING : InventoryAction.COLLECT_TO_CURSOR;
            case QUICK_CRAFT, UNKNOWN -> InventoryAction.UNKNOWN;
        };
    }

    private static InventoryAction pickupAction(ItemStack currentItem, ItemStack cursorItem) {
        if (currentItem.empty()) {
            return cursorItem.empty() ? InventoryAction.NOTHING : InventoryAction.PLACE_ALL;
        }
        if (cursorItem.empty()) {
            return InventoryAction.PICKUP_ALL;
        }
        if (similar(currentItem, cursorItem) && currentItem.amount() < currentItem.maxStackSize()) {
            return InventoryAction.PLACE_SOME;
        }
        return InventoryAction.SWAP_WITH_CURSOR;
    }

    private static InventoryAction pickupHalfAction(ItemStack currentItem, ItemStack cursorItem) {
        if (currentItem.empty()) {
            return cursorItem.empty() ? InventoryAction.NOTHING : InventoryAction.PLACE_ONE;
        }
        if (cursorItem.empty()) {
            return InventoryAction.PICKUP_HALF;
        }
        if (similar(currentItem, cursorItem) && currentItem.amount() < currentItem.maxStackSize()) {
            return InventoryAction.PLACE_ONE;
        }
        return InventoryAction.SWAP_WITH_CURSOR;
    }

    private static boolean similar(ItemStack currentItem, ItemStack cursorItem) {
        return !currentItem.empty()
                && !cursorItem.empty()
                && currentItem.type().equals(cursorItem.type())
                && currentItem.components().equals(cursorItem.components());
    }

    private static InventoryAction dropAction(boolean outside, ItemStack currentItem, ItemStack cursorItem, boolean all) {
        if (outside) {
            if (cursorItem.empty()) {
                return InventoryAction.NOTHING;
            }
            return all ? InventoryAction.DROP_ALL_CURSOR : InventoryAction.DROP_ONE_CURSOR;
        }
        if (currentItem.empty()) {
            return InventoryAction.NOTHING;
        }
        return all ? InventoryAction.DROP_ALL_SLOT : InventoryAction.DROP_ONE_SLOT;
    }
}
