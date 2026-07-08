package io.fand.api.event.entity;

import io.fand.api.entity.ItemEntity;
import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a non-player living entity picks up an item
 * entity.
 */
public final class EntityPickupItemEvent implements Event, Cancellable {

    private final LivingEntity entity;
    private final ItemEntity itemEntity;
    private ItemStack item;
    private boolean cancelled;

    public EntityPickupItemEvent(LivingEntity entity, ItemEntity itemEntity, ItemStack item) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.itemEntity = Objects.requireNonNull(itemEntity, "itemEntity");
        this.item = Objects.requireNonNull(item, "item");
    }

    public LivingEntity entity() {
        return entity;
    }

    public ItemEntity itemEntity() {
        return itemEntity;
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
