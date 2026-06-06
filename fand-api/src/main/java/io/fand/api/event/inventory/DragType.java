package io.fand.api.event.inventory;

/**
 * The variant of a quickcraft (drag) operation, distinguished by which mouse
 * button the player held. Mirrors vanilla's three internal drag modes.
 */
public enum DragType {
    /** Distributes the cursor stack evenly across the dragged slots, leaving
     *  any remainder on the cursor. Triggered by left-click drag. */
    EVEN,
    /** Places exactly one item in each dragged slot. Triggered by right-click drag. */
    SINGLE,
    /** Sets every dragged slot to a full stack copy of the cursor item.
     *  Only valid for players in creative mode. Triggered by middle-click drag. */
    CLONE
}
