package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import java.util.Objects;
import java.util.Optional;

/**
 * Typed target-change event fired when the new target is a living entity.
 */
public final class EntityTargetLivingEntityEvent extends EntityTargetEvent {

    private final LivingEntity target;

    public EntityTargetLivingEntityEvent(
            LivingEntity entity,
            Optional<LivingEntity> oldTarget,
            LivingEntity target,
            Cause cause) {
        super(entity, oldTarget, Optional.of(Objects.requireNonNull(target, "target")), cause);
        this.target = target;
    }

    public LivingEntity livingTarget() {
        return target;
    }
}
