package io.fand.server.inventory;

import io.fand.api.event.inventory.ClickType;
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
}
