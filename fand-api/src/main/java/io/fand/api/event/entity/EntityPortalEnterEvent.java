package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread when an entity enters a portal block.
 */
public record EntityPortalEnterEvent(Entity entity, Location location) implements Event {

    public EntityPortalEnterEvent(Entity entity, Location location) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.location = Objects.requireNonNull(location, "location");
    }
}
