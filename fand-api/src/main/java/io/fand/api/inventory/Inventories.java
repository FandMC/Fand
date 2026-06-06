package io.fand.api.inventory;

import io.fand.api.Fand;
import net.kyori.adventure.text.Component;

/**
 * Entry-point for plugins to construct standalone, server-side inventories
 * — the kind you can hand to {@code Player.openInventory(Inventory)} to
 * show a custom GUI.
 *
 * <p>Returned inventories support {@link Inventory#addSlotChangeListener},
 * carry the title you give them, and persist independently of any specific
 * viewer.
 */
public final class Inventories {

    private Inventories() {
    }

    /**
     * Creates a new empty inventory of the given {@code type} with the
     * default size for that type, titled {@code title}.
     */
    public static Inventory create(InventoryType type, Component title) {
        return create(type, 0, title);
    }

    /**
     * Creates a new empty inventory of the given {@code type} and size,
     * titled {@code title}. {@code size} follows the same rules as
     * {@code Player.openInventory(InventoryType, int)} — pass {@code 0}
     * for the type's default.
     *
     * @throws IllegalArgumentException if {@code size} is invalid for the
     *         requested {@code type}, or if {@code type} cannot be opened
     *         standalone (PLAYER, UNKNOWN, anvil/furnace/etc.)
     */
    public static Inventory create(InventoryType type, int size, Component title) {
        return Fand.server().createInventory(type, size, title);
    }
}
