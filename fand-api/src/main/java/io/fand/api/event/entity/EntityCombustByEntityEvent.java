package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import java.util.Objects;

/**
 * Fired on the server thread before another entity sets an entity on fire.
 */
public final class EntityCombustByEntityEvent extends EntityCombustEvent {

    private final Entity combuster;

    public EntityCombustByEntityEvent(Entity entity, Entity combuster, Cause cause, float durationSeconds) {
        super(entity, Objects.requireNonNull(combuster, "combuster"), cause, durationSeconds);
        this.combuster = combuster;
    }

    public Entity combuster() {
        return combuster;
    }
}
