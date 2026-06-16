package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Villager;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before vanilla applies a villager reputation
 * change such as hurt, kill witness, trade, or cure gossip.
 */
public final class VillagerReputationEvent implements Event, Cancellable {

    private final Villager villager;
    private final Entity source;
    private final Cause cause;
    private boolean cancelled;

    public VillagerReputationEvent(Villager villager, Entity source, Cause cause) {
        this.villager = Objects.requireNonNull(villager, "villager");
        this.source = Objects.requireNonNull(source, "source");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public Villager villager() {
        return villager;
    }

    public Entity source() {
        return source;
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
        ZOMBIE_VILLAGER_CURED,
        GOLEM_KILLED,
        VILLAGER_HURT,
        VILLAGER_KILLED,
        TRADE,
        UNKNOWN
    }
}
