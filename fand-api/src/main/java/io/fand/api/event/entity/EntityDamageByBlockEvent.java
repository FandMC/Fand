package io.fand.api.event.entity;

import io.fand.api.block.Block;
import io.fand.api.entity.LivingEntity;
import java.util.Map;
import java.util.Objects;

/**
 * Fired on the server thread before a block source damages a living entity.
 */
public final class EntityDamageByBlockEvent extends EntityDamageEvent {

    private final Block damager;

    public EntityDamageByBlockEvent(LivingEntity entity, String cause, double amount, Block damager) {
        super(entity, cause, amount, null, null);
        this.damager = Objects.requireNonNull(damager, "damager");
    }

    public EntityDamageByBlockEvent(
            LivingEntity entity,
            String cause,
            double amount,
            Map<DamageModifier, Double> modifiers,
            Block damager) {
        super(entity, DamageCause.of(cause), amount, modifiers, null, null);
        this.damager = Objects.requireNonNull(damager, "damager");
    }

    public Block damager() {
        return damager;
    }
}
