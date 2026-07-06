package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread when a non-player entity is removed from tracking.
 */
public record EntityRemoveEvent(Entity entity, Cause cause) implements Event {

    public enum Cause {
        KILLED,
        DISCARDED,
        UNLOADED_TO_CHUNK,
        UNLOADED_WITH_PLAYER,
        CHANGED_DIMENSION,
        UNKNOWN
    }

    public EntityRemoveEvent(Entity entity, Cause cause) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.cause = Objects.requireNonNull(cause, "cause");
    }
}
