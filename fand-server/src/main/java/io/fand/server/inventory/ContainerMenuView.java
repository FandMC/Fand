package io.fand.server.inventory;

import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Read/write view over an open {@link AbstractContainerMenu}. Slot indices
 * line up with vanilla's slot ordering: the container's own slots come first,
 * followed by the player's inventory at the upper end.
 *
 * <p>{@link #add(ItemStack)} is not supported on a generic menu view because
 * vanilla menus don't define an "insert here" path; it throws
 * {@link UnsupportedOperationException}.
 */
public final class ContainerMenuView implements Inventory {

    private final AbstractContainerMenu menu;
    private final InventoryType type;

    public ContainerMenuView(AbstractContainerMenu menu) {
        this.menu = menu;
        this.type = InventoryTypes.resolve(menu);
    }

    @Override
    public InventoryType type() {
        return type;
    }

    @Override
    public int size() {
        return menu.slots.size();
    }

    @Override
    public ItemStack get(int slot) {
        if (slot < 0 || slot >= menu.slots.size()) {
            return ItemStack.EMPTY;
        }
        return FandItemStacks.fromVanilla(menu.slots.get(slot).getItem());
    }

    @Override
    public void set(int slot, ItemStack stack) {
        if (slot < 0 || slot >= menu.slots.size()) {
            return;
        }
        menu.slots.get(slot).set(FandItemStacks.toVanilla(stack));
    }

    @Override
    public ItemStack add(ItemStack stack) {
        throw new UnsupportedOperationException("add() is not defined on a generic container menu");
    }

    @Override
    public void clear() {
        for (var slot : menu.slots) {
            slot.set(net.minecraft.world.item.ItemStack.EMPTY);
        }
    }
}
