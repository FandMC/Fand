package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import net.kyori.adventure.text.Component;

/**
 * Fired on the server thread before the server actively disconnects a player.
 */
public final class PlayerKickEvent implements Event, Cancellable {

    private final Player player;
    private Component reason;
    private boolean cancelled;

    public PlayerKickEvent(Player player, Component reason) {
        this.player = Objects.requireNonNull(player, "player");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public Player player() {
        return player;
    }

    public Component reason() {
        return reason;
    }

    public void setReason(Component reason) {
        this.reason = Objects.requireNonNull(reason, "reason");
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
