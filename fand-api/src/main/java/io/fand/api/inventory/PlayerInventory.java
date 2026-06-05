package io.fand.api.inventory;

import io.fand.api.item.ItemStack;

/**
 * A player's main inventory plus hotbar. Slots 0–8 are the hotbar in vanilla
 * order (left to right); slots 9–35 are the upper rows.
 */
public interface PlayerInventory extends Inventory {

    /** Currently selected hotbar slot (0–8). */
    int selectedSlot();

    /** Sets the selected hotbar slot; {@code slot} must be in {@code [0, 8]}. */
    void setSelectedSlot(int slot);

    /** Stack currently held in the player's main hand. */
    ItemStack heldItem();
}
