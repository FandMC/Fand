package io.fand.api.event.player;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before a player attaches a leash to an entity.
 */
public final class PlayerLeashEntityEvent implements Event, Cancellable {

    private final Player player;
    private final Entity entity;
    private final @Nullable Entity holder;
    private final Cause cause;
    private boolean cancelled;

    public PlayerLeashEntityEvent(Player player, Entity entity, @Nullable Entity holder, Cause cause) {
        this.player = Objects.requireNonNull(player, "player");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.holder = holder;
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public Player player() {
        return player;
    }

    public Entity entity() {
        return entity;
    }

    public Optional<Entity> holder() {
        return Optional.ofNullable(holder);
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
        PLAYER,
        FENCE,
        TRANSFER
    }
}
