package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a non-player entity is added to a world.
 */
public final class EntitySpawnEvent implements Event, Cancellable {

    public enum Cause {
        NATURAL,
        CHUNK_GENERATION,
        SPAWNER,
        STRUCTURE,
        BREEDING,
        MOB_SUMMONED,
        JOCKEY,
        EVENT,
        CONVERSION,
        REINFORCEMENT,
        TRIGGERED,
        BUCKET,
        SPAWN_ITEM_USE,
        COMMAND,
        DISPENSER,
        PATROL,
        TRIAL_SPAWNER,
        LOAD,
        DIMENSION_TRAVEL,
        CUSTOM,
        UNKNOWN
    }

    private final Entity entity;
    private final Cause cause;
    private boolean cancelled;

    public EntitySpawnEvent(Entity entity, Cause cause) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public Entity entity() {
        return entity;
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
