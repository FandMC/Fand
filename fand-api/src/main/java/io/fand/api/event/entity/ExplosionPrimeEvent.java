package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before an explosion is created.
 */
public final class ExplosionPrimeEvent implements Event, Cancellable {

    private final Location location;
    private final @Nullable Entity source;
    private float radius;
    private boolean fire;
    private boolean cancelled;

    public ExplosionPrimeEvent(Location location, @Nullable Entity source, float radius, boolean fire) {
        this.location = Objects.requireNonNull(location, "location");
        this.source = source;
        this.radius = radius;
        this.fire = fire;
    }

    public Location location() {
        return location;
    }

    public Optional<Entity> source() {
        return Optional.ofNullable(source);
    }

    public float radius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = Math.max(0.0F, radius);
    }

    public boolean fire() {
        return fire;
    }

    public void setFire(boolean fire) {
        this.fire = fire;
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
