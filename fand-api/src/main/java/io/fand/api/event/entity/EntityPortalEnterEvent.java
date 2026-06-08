package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread when an entity enters a portal block.
 */
public final class EntityPortalEnterEvent implements Event {

    private final Entity entity;
    private final Location location;

    public EntityPortalEnterEvent(Entity entity, Location location) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.location = Objects.requireNonNull(location, "location");
    }

    public Entity entity() {
        return entity;
    }

    public Location location() {
        return location;
    }
}
