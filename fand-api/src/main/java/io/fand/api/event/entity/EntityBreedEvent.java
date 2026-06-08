package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread after vanilla creates a breeding offspring entity
 * but before it is spawned into the world.
 */
public final class EntityBreedEvent implements Event, Cancellable {

    private final LivingEntity parent;
    private final LivingEntity partner;
    private final Optional<Player> breeder;
    private final LivingEntity child;
    private boolean cancelled;

    public EntityBreedEvent(LivingEntity parent, LivingEntity partner, Optional<Player> breeder, LivingEntity child) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.partner = Objects.requireNonNull(partner, "partner");
        this.breeder = Objects.requireNonNull(breeder, "breeder");
        this.child = Objects.requireNonNull(child, "child");
    }

    public LivingEntity parent() {
        return parent;
    }

    public LivingEntity partner() {
        return partner;
    }

    public Optional<Player> breeder() {
        return breeder;
    }

    public LivingEntity child() {
        return child;
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
