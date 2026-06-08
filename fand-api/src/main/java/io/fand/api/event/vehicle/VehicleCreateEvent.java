package io.fand.api.event.vehicle;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a vehicle entity is added to a world.
 */
public final class VehicleCreateEvent implements Event, Cancellable {

    private final Entity vehicle;
    private boolean cancelled;

    public VehicleCreateEvent(Entity vehicle) {
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
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
