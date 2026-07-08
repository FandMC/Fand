package io.fand.api.event.entity;

import io.fand.api.entity.ItemEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a dropped item despawns because of age.
 */
public final class ItemDespawnEvent implements Event, Cancellable {

    private final ItemEntity entity;
    private final ItemStack item;
    private final int age;
    private boolean cancelled;

    public ItemDespawnEvent(ItemEntity entity, ItemStack item, int age) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.item = Objects.requireNonNull(item, "item");
        this.age = Math.max(0, age);
    }

    public ItemEntity entity() {
        return entity;
    }

    public ItemStack item() {
        return item;
    }

    public int age() {
        return age;
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
