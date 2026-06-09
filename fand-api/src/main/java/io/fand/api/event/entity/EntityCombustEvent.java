package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before an entity is set on fire.
 */
public class EntityCombustEvent implements Event, Cancellable {

    private final Entity entity;
    private final @Nullable Entity source;
    private final Cause cause;
    private float durationSeconds;
    private boolean cancelled;

    public EntityCombustEvent(Entity entity, @Nullable Entity source, Cause cause, float durationSeconds) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.source = source;
        this.cause = Objects.requireNonNull(cause, "cause");
        this.durationSeconds = Math.max(0.0F, durationSeconds);
    }

    public Entity entity() {
        return entity;
    }

    public Optional<Entity> source() {
        return Optional.ofNullable(source);
    }

    public Cause cause() {
        return cause;
    }

    public float durationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(float durationSeconds) {
        this.durationSeconds = Math.max(0.0F, durationSeconds);
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
        FIRE,
        LAVA,
        ATTACK,
        PROJECTILE,
        UNKNOWN
    }
}
