package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before a hanging entity is broken.
 */
public final class HangingBreakEvent implements Event, Cancellable {

    public enum Cause {
        ENTITY,
        EXPLOSION,
        PHYSICS,
        OBSTRUCTION,
        UNKNOWN
    }

    private final Entity entity;
    private final @Nullable Entity remover;
    private final Cause cause;
    private boolean cancelled;

    public HangingBreakEvent(Entity entity, @Nullable Entity remover, Cause cause) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.remover = remover;
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public Entity entity() {
        return entity;
    }

    public Optional<Entity> remover() {
        return Optional.ofNullable(remover);
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
