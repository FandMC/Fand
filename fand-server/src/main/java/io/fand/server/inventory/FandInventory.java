package io.fand.server.inventory;

import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.api.inventory.SlotChangeListener;
import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.kyori.adventure.text.Component;
import net.minecraft.world.SimpleContainer;

/**
 * Standalone server-side inventory created by plugins — backed by a
 * {@link SimpleContainer} that fires slot-change notifications back to
 * registered listeners.
 *
 * <p>The same instance can be opened by multiple players, but vanilla's
 * menu wiring binds it to a single open menu at a time. Holding a long-
 * lived reference is safe; the inventory does not become invalid when its
 * viewer closes the menu.
 */
public final class FandInventory implements Inventory {

    private final InventoryType type;
    private final Component title;
    private final NotifyingContainer container;
    private final CopyOnWriteArrayList<SlotChangeListener> listeners = new CopyOnWriteArrayList<>();

    public FandInventory(InventoryType type, int size, Component title) {
        this.type = Objects.requireNonNull(type, "type");
        this.title = Objects.requireNonNull(title, "title");
        this.container = new NotifyingContainer(size, this);
    }

    public NotifyingContainer container() {
        return container;
    }

    @Override
    public InventoryType type() {
        return type;
    }

    @Override
    public Component title() {
        return title;
    }

    @Override
    public int size() {
        return container.getContainerSize();
    }

    @Override
    public ItemStack get(int slot) {
        return FandItemStacks.fromVanilla(container.getItem(slot));
    }

    @Override
    public void set(int slot, ItemStack stack) {
        container.setItem(slot, FandItemStacks.toVanilla(stack));
    }

    @Override
    public ItemStack add(ItemStack stack) {
        if (stack == null || stack.empty()) {
            return ItemStack.EMPTY;
        }
        var leftover = container.addItem(FandItemStacks.toVanilla(stack));
        return FandItemStacks.fromVanilla(leftover);
    }

    @Override
    public void clear() {
        container.clearContent();
    }

    @Override
    public AutoCloseable addSlotChangeListener(SlotChangeListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    void notifySlotChanged(int slot, net.minecraft.world.item.ItemStack oldStack, net.minecraft.world.item.ItemStack newStack) {
        if (listeners.isEmpty()) {
            return;
        }
        var oldApi = FandItemStacks.fromVanilla(oldStack);
        var newApi = FandItemStacks.fromVanilla(newStack);
        for (var listener : listeners) {
            try {
                listener.onSlotChange(slot, oldApi, newApi);
            } catch (RuntimeException failure) {
                org.slf4j.LoggerFactory.getLogger(FandInventory.class)
                        .warn("SlotChangeListener failed", failure);
            }
        }
    }

    /** SimpleContainer subclass that captures pre-mutation state and notifies the parent. */
    public static final class NotifyingContainer extends SimpleContainer {
        private final FandInventory parent;

        NotifyingContainer(int size, FandInventory parent) {
            super(size);
            this.parent = parent;
        }

        @Override
        public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
            var oldStack = this.getItem(slot).copy();
            super.setItem(slot, stack);
            parent.notifySlotChanged(slot, oldStack, this.getItem(slot).copy());
        }

        @Override
        public net.minecraft.world.item.ItemStack removeItem(int slot, int amount) {
            var oldStack = this.getItem(slot).copy();
            var removed = super.removeItem(slot, amount);
            parent.notifySlotChanged(slot, oldStack, this.getItem(slot).copy());
            return removed;
        }

        @Override
        public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
            var oldStack = this.getItem(slot).copy();
            var removed = super.removeItemNoUpdate(slot);
            parent.notifySlotChanged(slot, oldStack, this.getItem(slot).copy());
            return removed;
        }
    }
}
