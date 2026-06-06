package io.fand.server.inventory;

import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import java.util.Objects;
import net.minecraft.world.Container;

/**
 * View over a vanilla {@link Container} as an API-side {@link Inventory}.
 * Used for transient containers opened via {@code Player.openInventory}.
 */
public final class FandContainerInventory implements Inventory {

    private final Container handle;
    private final InventoryType type;

    public FandContainerInventory(Container handle, InventoryType type) {
        this.handle = Objects.requireNonNull(handle, "handle");
        this.type = Objects.requireNonNull(type, "type");
    }

    public Container handle() {
        return handle;
    }

    @Override
    public InventoryType type() {
        return type;
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
        if (handle instanceof net.minecraft.world.SimpleContainer simple) {
            var leftover = simple.addItem(FandItemStacks.toVanilla(stack));
            return FandItemStacks.fromVanilla(leftover);
        }
        for (int slot = 0; slot < handle.getContainerSize(); slot++) {
            if (handle.getItem(slot).isEmpty()) {
                handle.setItem(slot, FandItemStacks.toVanilla(stack));
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    @Override
    public void clear() {
        handle.clearContent();
    }
}
