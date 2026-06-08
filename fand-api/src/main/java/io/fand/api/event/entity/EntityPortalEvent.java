package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread before a non-player entity uses a portal.
 *
 * <p>Listeners may cancel the portal transfer or replace the destination.
 * Player portal transfers use {@link io.fand.api.event.player.PlayerPortalEvent}.
 */
public final class EntityPortalEvent implements Event, Cancellable {

    private final Entity entity;
    private final Location from;
    private Location to;
    private boolean cancelled;

    public EntityPortalEvent(Entity entity, Location from, Location to) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
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

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
