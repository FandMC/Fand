package io.fand.api.event.inventory;

/**
 * What a player did in an inventory click. Mapped from the vanilla
 * {@code ContainerInput} + button number combination so each enum value
 * uniquely describes the gesture.
 *
 * <p>Vanilla's quick-craft (drag) protocol sends multiple {@code QUICK_CRAFT}
 * packets; only the per-click events are surfaced here, not the synthesized
 * drop that follows. Drop-from-cursor (clicking outside any slot with a
 * carried stack) is reported as {@link #DROP} or {@link #DROP_ALL}.
 */
public enum ClickType {
    /** Left-click on a slot. */
    PICKUP,
    /** Right-click on a slot. */
    PICKUP_HALF,
    /** Shift-left-click. */
    QUICK_MOVE,
    /** Shift-right-click. Vanilla treats this identically to {@link #QUICK_MOVE}. */
    QUICK_MOVE_ALL,
    /** Number-key swap with a hotbar slot (button = hotbar index 0-8). */
    SWAP,
    /** Swap with the off-hand slot. */
    SWAP_OFFHAND,
    /** Middle-click clone (creative only). */
    CLONE,
    /** Q (drop one) on a slot. */
    DROP,
    /** Ctrl-Q (drop stack) on a slot, or left-click drop with carried cursor outside slots. */
    DROP_ALL,
    /** A single quick-craft (drag) packet — start, add-slot, or end. */
    QUICK_CRAFT,
    /** Double-click pickup-all of matching items. */
    PICKUP_ALL,
    /** Anything we cannot classify — surfaced so listeners can choose to abstain. */
    UNKNOWN
}
