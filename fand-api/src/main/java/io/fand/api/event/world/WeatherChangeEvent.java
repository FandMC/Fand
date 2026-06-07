package io.fand.api.event.world;

import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.World;
import java.util.Objects;

/**
 * Fired on the server thread before a world's rain state changes.
 */
public final class WeatherChangeEvent implements Event, Cancellable {

    private final World world;
    private final boolean fromStorm;
    private final boolean toStorm;
    private boolean cancelled;

    public WeatherChangeEvent(World world, boolean fromStorm, boolean toStorm) {
        this.world = Objects.requireNonNull(world, "world");
        this.fromStorm = fromStorm;
        this.toStorm = toStorm;
    }

    public World world() {
        return world;
    }

    public boolean fromStorm() {
        return fromStorm;
    }

    public boolean toStorm() {
        return toStorm;
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
