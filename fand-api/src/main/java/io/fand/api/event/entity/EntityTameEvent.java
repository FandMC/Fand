package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a tameable entity becomes owned by a player.
 */
public final class EntityTameEvent implements Event, Cancellable {

    private final LivingEntity entity;
    private final Player owner;
    private boolean cancelled;

    public EntityTameEvent(LivingEntity entity, Player owner) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public LivingEntity entity() {
        return entity;
    }

    public Player owner() {
        return owner;
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
