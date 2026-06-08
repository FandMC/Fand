package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Fired on the server thread before a mob converts into another entity type.
 */
public final class EntityTransformEvent implements Event, Cancellable {

    private final Entity entity;
    private final Key targetType;
    private final Cause cause;
    private boolean cancelled;

    public EntityTransformEvent(Entity entity, Key targetType, Cause cause) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.targetType = Objects.requireNonNull(targetType, "targetType");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public Entity entity() {
        return entity;
    }

    public Key targetType() {
        return targetType;
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

    public enum Cause {
        CONVERSION,
        SPLIT,
        UNKNOWN
    }
}
