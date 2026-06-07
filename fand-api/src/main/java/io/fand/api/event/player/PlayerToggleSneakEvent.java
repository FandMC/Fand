package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a player changes their sneak state.
 */
public final class PlayerToggleSneakEvent implements Event, Cancellable {

    private final Player player;
    private final boolean sneaking;
    private boolean cancelled;

    public PlayerToggleSneakEvent(Player player, boolean sneaking) {
        this.player = Objects.requireNonNull(player, "player");
        this.sneaking = sneaking;
    }

    public Player player() {
        return player;
    }

    public boolean sneaking() {
        return sneaking;
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
