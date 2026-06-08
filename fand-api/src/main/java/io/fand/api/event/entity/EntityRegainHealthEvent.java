package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a living entity regains health.
 */
public final class EntityRegainHealthEvent implements Event, Cancellable {

    private final LivingEntity entity;
    private final Cause cause;
    private double amount;
    private boolean cancelled;

    public EntityRegainHealthEvent(LivingEntity entity, double amount, Cause cause) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.amount = Math.max(0.0, amount);
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public LivingEntity entity() {
        return entity;
    }

    public double amount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = Math.max(0.0, amount);
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
        UNKNOWN,
        NATURAL_REGEN,
        SATIATED,
        REGENERATION,
        MAGIC,
        EATING,
        WITHER,
        BEACON
    }
}
