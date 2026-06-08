package io.fand.api.event.vehicle;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a vehicle is destroyed by damage.
 */
public final class VehicleDestroyEvent implements Event, Cancellable {

    private final Entity vehicle;
    private final Optional<Entity> attacker;
    private boolean cancelled;

    public VehicleDestroyEvent(Entity vehicle, Optional<Entity> attacker) {
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.attacker = Objects.requireNonNull(attacker, "attacker");
    }

    public Entity vehicle() {
        return vehicle;
    }

    public Optional<Entity> attacker() {
        return attacker;
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
