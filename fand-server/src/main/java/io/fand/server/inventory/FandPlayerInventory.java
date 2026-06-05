package io.fand.server.inventory;

import io.fand.api.inventory.PlayerInventory;
import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import net.minecraft.world.entity.player.Inventory;

public final class FandPlayerInventory implements PlayerInventory {

    private final Inventory handle;

    public FandPlayerInventory(Inventory handle) {
        this.handle = handle;
    }

    @Override
    public int size() {
        return handle.getContainerSize();
    }

    @Override
    public ItemStack get(int slot) {
        return FandItemStacks.fromVanilla(handle.getItem(slot));
    }

    @Override
    public void set(int slot, ItemStack stack) {
        handle.setItem(slot, FandItemStacks.toVanilla(stack));
    }

    @Override
    public ItemStack add(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var vanilla = FandItemStacks.toVanilla(stack);
        handle.add(vanilla);
        return FandItemStacks.fromVanilla(vanilla);
    }

    @Override
    public void clear() {
        handle.clearContent();
    }

    @Override
    public int selectedSlot() {
        return handle.getSelectedSlot();
    }

    @Override
    public void setSelectedSlot(int slot) {
        if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException("Selected hotbar slot must be in [0, 8], got " + slot);
        }
        handle.setSelectedSlot(slot);
    }

    @Override
    public ItemStack heldItem() {
        return FandItemStacks.fromVanilla(handle.getSelectedItem());
    }
}
