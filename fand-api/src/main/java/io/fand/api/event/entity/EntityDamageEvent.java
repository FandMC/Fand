package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;

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
public class EntityDamageEvent implements Event, Cancellable {

    private final LivingEntity entity;
    private final String cause;
    private final Optional<LivingEntity> directEntity;
    private final Optional<LivingEntity> attacker;
    private double amount;
    private boolean cancelled;

    public EntityDamageEvent(LivingEntity entity, String cause, double amount) {
        this(entity, cause, amount, Optional.empty(), Optional.empty());
    }

    public EntityDamageEvent(
            LivingEntity entity,
            String cause,
            double amount,
            Optional<LivingEntity> directEntity,
            Optional<LivingEntity> attacker) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.directEntity = Objects.requireNonNull(directEntity, "directEntity");
        this.attacker = Objects.requireNonNull(attacker, "attacker");
        this.amount = amount;
    }

    public LivingEntity entity() {
        return entity;
    }

    /** Vanilla damage-type identifier (e.g. {@code minecraft:fall}, {@code minecraft:player_attack}). */
    public String cause() {
        return cause;
    }

    /**
     * Direct living entity involved in the damage, if any. For melee attacks
     * this is usually the attacker; for projectiles it is empty because the
     * direct source is an arrow/fireball rather than a living entity.
     */
    public Optional<LivingEntity> directEntity() {
        return directEntity;
    }

    /** Living entity credited as causing the damage, if vanilla exposes one. */
    public Optional<LivingEntity> attacker() {
        return attacker;
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
