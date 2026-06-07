package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a player changes their sprint state.
 */
public final class PlayerToggleSprintEvent implements Event, Cancellable {

    private final Player player;
    private final boolean sprinting;
    private boolean cancelled;

    public PlayerToggleSprintEvent(Player player, boolean sprinting) {
        this.player = Objects.requireNonNull(player, "player");
        this.sprinting = sprinting;
    }

    public Player player() {
        return player;
    }

    public boolean sprinting() {
        return sprinting;
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
