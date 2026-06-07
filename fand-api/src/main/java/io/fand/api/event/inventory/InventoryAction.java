package io.fand.api.event.inventory;

/**
 * Higher-level inventory mutation vanilla intends to perform for a click.
 *
 * <p>{@link ClickType} describes the physical input gesture. This enum
 * describes the resulting inventory action after considering the clicked slot
 * and carried cursor item.
 */
public enum InventoryAction {
    NOTHING,
    PICKUP_ALL,
    PICKUP_SOME,
    PICKUP_HALF,
    PICKUP_ONE,
    PLACE_ALL,
    PLACE_SOME,
    PLACE_ONE,
    SWAP_WITH_CURSOR,
    MOVE_TO_OTHER_INVENTORY,
    HOTBAR_SWAP,
    HOTBAR_MOVE_AND_READD,
    DROP_ALL_CURSOR,
    DROP_ONE_CURSOR,
    DROP_ALL_SLOT,
    DROP_ONE_SLOT,
    CLONE_STACK,
    COLLECT_TO_CURSOR,
    UNKNOWN
}
