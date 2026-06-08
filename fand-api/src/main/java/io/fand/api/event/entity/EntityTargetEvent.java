package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a mob changes its current target.
 */
public class EntityTargetEvent implements Event, Cancellable {

    private final LivingEntity entity;
    private final Optional<LivingEntity> oldTarget;
    private Optional<LivingEntity> target;
    private final Cause cause;
    private boolean cancelled;

    public EntityTargetEvent(
            LivingEntity entity,
            Optional<LivingEntity> oldTarget,
            Optional<LivingEntity> target,
            Cause cause) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.oldTarget = Objects.requireNonNull(oldTarget, "oldTarget");
        this.target = Objects.requireNonNull(target, "target");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public LivingEntity entity() {
        return entity;
    }

    public Optional<LivingEntity> oldTarget() {
        return oldTarget;
    }

    public Optional<LivingEntity> target() {
        return target;
    }

    public void setTarget(Optional<LivingEntity> target) {
        this.target = Objects.requireNonNull(target, "target");
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
        CLOSEST_ENTITY,
        TARGET_ATTACKED_ENTITY,
        OWNER_ATTACKED_TARGET,
        TARGET_ATTACKED_OWNER,
        FORGOT_TARGET,
        CUSTOM
    }
}
