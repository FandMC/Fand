package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread before a non-player entity exits a portal.
 */
public final class EntityPortalExitEvent extends EntityPortalEvent implements Cancellable {

    private Location after;

    public EntityPortalExitEvent(Entity entity, Location from, Location to, Location after) {
        super(entity, from, to);
        this.after = Objects.requireNonNull(after, "after");
    }

    public Location after() {
        return after;
    }

    public void setAfter(Location after) {
        this.after = Objects.requireNonNull(after, "after");
    }
}
