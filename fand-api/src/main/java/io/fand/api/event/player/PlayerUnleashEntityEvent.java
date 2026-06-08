package io.fand.api.event.player;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a player removes a leash from an entity.
 */
public final class PlayerUnleashEntityEvent implements Event, Cancellable {

    private final Player player;
    private final Entity entity;
    private final Optional<Entity> holder;
    private final boolean dropLead;
    private boolean cancelled;

    public PlayerUnleashEntityEvent(Player player, Entity entity, Optional<Entity> holder, boolean dropLead) {
        this.player = Objects.requireNonNull(player, "player");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.holder = Objects.requireNonNull(holder, "holder");
        this.dropLead = dropLead;
    }

    public Player player() {
        return player;
    }

    public Entity entity() {
        return entity;
    }

    public Optional<Entity> holder() {
        return holder;
    }

    public boolean dropLead() {
        return dropLead;
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
