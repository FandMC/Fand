package io.fand.api.event.entity;

import io.fand.api.block.Block;
import io.fand.api.entity.Entity;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a block source sets an entity on fire.
 */
public final class EntityCombustByBlockEvent extends EntityCombustEvent {

    private final Block combuster;

    public EntityCombustByBlockEvent(Entity entity, Block combuster, Cause cause, float durationSeconds) {
        super(entity, Optional.empty(), cause, durationSeconds);
        this.combuster = Objects.requireNonNull(combuster, "combuster");
    }

    public Block combuster() {
        return combuster;
    }
}
