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

    /** Replaces the stack currently held in the player's main hand. */
    void setHeldItem(ItemStack stack);

    /** Stack currently held in the player's off hand. */
    ItemStack offhandItem();

    /** Replaces the stack currently held in the player's off hand. */
    void setOffhandItem(ItemStack stack);

    /** Helmet slot. */
    ItemStack helmet();

    /** Replaces the helmet slot. */
    void setHelmet(ItemStack stack);

    /** Chestplate slot. */
    ItemStack chestplate();

    /** Replaces the chestplate slot. */
    void setChestplate(ItemStack stack);

    /** Leggings slot. */
    ItemStack leggings();

    /** Replaces the leggings slot. */
    void setLeggings(ItemStack stack);

    /** Boots slot. */
    ItemStack boots();

    /** Replaces the boots slot. */
    void setBoots(ItemStack stack);
}
