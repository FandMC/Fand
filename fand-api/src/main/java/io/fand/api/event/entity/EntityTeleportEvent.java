package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread before a non-player entity teleports.
 */
public final class EntityTeleportEvent implements Event, Cancellable {

    public enum Cause {
        PORTAL,
        DIMENSION_CHANGE,
        COMMAND,
        PLUGIN,
        UNKNOWN
    }

    private final Entity entity;
    private final Location from;
    private final Cause cause;
    private Location to;
    private boolean cancelled;

    public EntityTeleportEvent(Entity entity, Location from, Location to, Cause cause) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public Entity entity() {
        return entity;
    }

    public Location from() {
        return from;
    }

    public Location to() {
        return to;
    }

    public void setTo(Location to) {
        this.to = Objects.requireNonNull(to, "to");
    }

    public Cause cause() {
        return cause;
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
