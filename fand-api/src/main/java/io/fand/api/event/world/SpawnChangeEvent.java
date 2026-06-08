package io.fand.api.event.world;

import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread before the global spawn location changes.
 */
public final class SpawnChangeEvent implements Event, Cancellable {

    private final Location previousSpawn;
    private Location newSpawn;
    private boolean cancelled;

    public SpawnChangeEvent(Location previousSpawn, Location newSpawn) {
        this.previousSpawn = Objects.requireNonNull(previousSpawn, "previousSpawn");
        this.newSpawn = Objects.requireNonNull(newSpawn, "newSpawn");
    }

    public Location previousSpawn() {
        return previousSpawn;
    }

    public Location newSpawn() {
        return newSpawn;
    }

    public void setNewSpawn(Location newSpawn) {
        this.newSpawn = Objects.requireNonNull(newSpawn, "newSpawn");
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
