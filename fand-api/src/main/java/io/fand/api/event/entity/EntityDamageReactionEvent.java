package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before vanilla applies a reaction caused by a
 * damage flow, such as hurt memory, revenge targeting, or reputation changes.
 *
 * <p>Plugins can cancel negative reactions while still allowing the underlying
 * hit feedback and other unrelated game behavior to continue.
 */
public final class EntityDamageReactionEvent implements Event, Cancellable {

    private final LivingEntity entity;
    private final @Nullable Entity source;
    private final Cause cause;
    private final Impact impact;
    private boolean cancelled;

    public EntityDamageReactionEvent(
            LivingEntity entity,
            @Nullable Entity source,
            Cause cause,
            Impact impact) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.source = source;
        this.cause = Objects.requireNonNull(cause, "cause");
        this.impact = Objects.requireNonNull(impact, "impact");
    }

    /** Entity whose reaction state would change. */
    public LivingEntity entity() {
        return entity;
    }

    /** Entity that caused the reaction, when vanilla exposes one. */
    public Optional<Entity> source() {
        return Optional.ofNullable(source);
    }

    public Cause cause() {
        return cause;
    }

    public Impact impact() {
        return impact;
    }

    public boolean negative() {
        return impact == Impact.NEGATIVE;
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
        HURT_BY_MOB,
        HURT_BY_PLAYER,
        HURT_MEMORY,
        ENTITY_TARGET,
        VILLAGER_REPUTATION
    }

    public enum Impact {
        NEGATIVE,
        NEUTRAL,
        POSITIVE
    }
}
