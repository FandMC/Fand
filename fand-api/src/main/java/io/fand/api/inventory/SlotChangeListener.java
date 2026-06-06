package io.fand.api.inventory;

import io.fand.api.item.ItemStack;

/**
 * Notified when a slot in a Fand-created inventory changes contents.
 * Fired on the server thread, after the new stack is in place.
 *
 * <p>Only inventories created via {@link Inventories#create} support
 * listeners — {@link PlayerInventory} and inventories surfaced by vanilla
 * container menus do not.
 */
@FunctionalInterface
public interface SlotChangeListener {

    void onSlotChange(int slot, ItemStack oldStack, ItemStack newStack);
}
