package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread before an entity-spawned item entity is added to
 * the world.
 */
public final class EntityDropItemEvent implements Event, Cancellable {

    private final Entity entity;
    private final Location location;
    private ItemStack item;
    private boolean cancelled;

    public EntityDropItemEvent(Entity entity, Location location, ItemStack item) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.location = Objects.requireNonNull(location, "location");
        this.item = Objects.requireNonNull(item, "item");
    }

    public Entity entity() {
        return entity;
    }

    public Location location() {
        return location;
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
