package io.fand.api.event.world;

import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.World;
import java.util.Objects;

/**
 * Fired on the server thread before a world's thunder state changes.
 */
public final class ThunderChangeEvent implements Event, Cancellable {

    private final World world;
    private final boolean fromThundering;
    private final boolean toThundering;
    private boolean cancelled;

    public ThunderChangeEvent(World world, boolean fromThundering, boolean toThundering) {
        this.world = Objects.requireNonNull(world, "world");
        this.fromThundering = fromThundering;
        this.toThundering = toThundering;
    }

    public World world() {
        return world;
    }

    public boolean fromThundering() {
        return fromThundering;
    }

    public boolean toThundering() {
        return toThundering;
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
