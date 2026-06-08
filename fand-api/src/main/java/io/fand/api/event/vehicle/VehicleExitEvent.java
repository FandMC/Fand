package io.fand.api.event.vehicle;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before an entity exits a vehicle.
 */
public final class VehicleExitEvent implements Event, Cancellable {

    private final Entity vehicle;
    private final Entity entity;
    private boolean cancelled;

    public VehicleExitEvent(Entity vehicle, Entity entity) {
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.entity = Objects.requireNonNull(entity, "entity");
    }

    public Entity vehicle() {
        return vehicle;
    }

    public Entity entity() {
        return entity;
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
