package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before vanilla applies damage to a
 * {@link LivingEntity}. Cancelling the event aborts the damage application:
 * the victim keeps their current health, no knockback is dealt, and damage
 * immunity timers are not advanced.
 *
 * <p>The damage amount is mutable and reflects the post-armor, post-effect
 * value vanilla intends to apply. Setting it to zero is equivalent to
 * cancelling. Negative values are clamped to zero by the runtime.
 */
public final class EntityDamageEvent implements Event, Cancellable {

    private final LivingEntity entity;
    private final String cause;
    private double amount;
    private boolean cancelled;

    public EntityDamageEvent(LivingEntity entity, String cause, double amount) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.amount = amount;
    }

    public LivingEntity entity() {
        return entity;
    }

    /** Vanilla damage-type identifier (e.g. {@code minecraft:fall}, {@code minecraft:player_attack}). */
    public String cause() {
        return cause;
    }

    public double amount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
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
