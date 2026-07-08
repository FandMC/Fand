package io.fand.api.event.inventory;

import io.fand.api.entity.ItemEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before an inventory automation block picks up an
 * item entity from the world.
 */
public final class InventoryPickupItemEvent implements Event, Cancellable {

    private final Inventory inventory;
    private final @Nullable ItemEntity itemEntity;
    private ItemStack item;
    private boolean cancelled;

    public InventoryPickupItemEvent(Inventory inventory, @Nullable ItemEntity itemEntity, ItemStack item) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.itemEntity = itemEntity;
        this.item = Objects.requireNonNull(item, "item");
    }

    public Inventory inventory() {
        return inventory;
    }

    public Optional<ItemEntity> itemEntity() {
        return Optional.ofNullable(itemEntity);
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = Objects.requireNonNull(item, "item");
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
