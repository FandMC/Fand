package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a dropped item entity is added to a world.
 */
public final class ItemSpawnEvent implements Event, Cancellable {

    private final Entity entity;
    private ItemStack item;
    private boolean cancelled;

    public ItemSpawnEvent(Entity entity, ItemStack item) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.item = Objects.requireNonNull(item, "item");
    }

    public Entity entity() {
        return entity;
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
