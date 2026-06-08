package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before an entity starts riding another entity.
 */
public final class EntityMountEvent implements Event, Cancellable {

    private final Entity entity;
    private final Entity vehicle;
    private boolean cancelled;

    public EntityMountEvent(Entity entity, Entity vehicle) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
    }

    public Entity entity() {
        return entity;
    }

    public Entity vehicle() {
        return vehicle;
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
