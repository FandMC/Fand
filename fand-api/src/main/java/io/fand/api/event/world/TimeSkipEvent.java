package io.fand.api.event.world;

import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.World;
import java.util.Objects;

/**
 * Fired on the server thread before vanilla skips world time, such as when
 * enough players sleep.
 */
public final class TimeSkipEvent implements Event, Cancellable {

    private final World world;
    private final Cause cause;
    private final long fromTime;
    private long toTime;
    private boolean cancelled;

    public TimeSkipEvent(World world, Cause cause, long fromTime, long toTime) {
        this.world = Objects.requireNonNull(world, "world");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.fromTime = fromTime;
        this.toTime = toTime;
    }

    public World world() {
        return world;
    }

    public Cause cause() {
        return cause;
    }

    public long fromTime() {
        return fromTime;
    }

    public long toTime() {
        return toTime;
    }

    public void setToTime(long toTime) {
        this.toTime = toTime;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public enum Cause {
        SLEEP,
        COMMAND,
        UNKNOWN
    }
}
