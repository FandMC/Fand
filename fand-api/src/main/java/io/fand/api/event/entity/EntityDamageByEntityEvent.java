package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before one living entity damages another.
 *
 * <p>This is a typed specialization of {@link EntityDamageEvent}; listeners
 * registered for {@code EntityDamageEvent} also receive this event. Cancelling
 * or changing the amount has the same effect as the generic damage event.
 */
public class EntityDamageByEntityEvent extends EntityDamageEvent {

    private final LivingEntity damager;

    public EntityDamageByEntityEvent(
            LivingEntity entity,
            String cause,
            double amount,
            LivingEntity damager,
            @Nullable LivingEntity directEntity) {
        super(entity, cause, amount, directEntity, Objects.requireNonNull(damager, "damager"));
        this.damager = damager;
    }

    public EntityDamageByEntityEvent(
            LivingEntity entity,
            String cause,
            double amount,
            Map<DamageModifier, Double> modifiers,
            LivingEntity damager,
            @Nullable LivingEntity directEntity) {
        super(entity, DamageCause.of(cause), amount, modifiers, directEntity, Objects.requireNonNull(damager, "damager"));
        this.damager = damager;
    }

    /** Living entity credited by vanilla as causing this damage. */
    public LivingEntity damager() {
        return damager;
    }
}
