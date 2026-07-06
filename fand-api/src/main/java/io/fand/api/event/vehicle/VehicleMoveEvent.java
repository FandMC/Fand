package io.fand.api.event.vehicle;

import io.fand.api.entity.Entity;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread after a vehicle changes position.
 */
public record VehicleMoveEvent(Entity vehicle, Location from, Location to) implements Event {

    public VehicleMoveEvent(Entity vehicle, Location from, Location to) {
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
    }
}
