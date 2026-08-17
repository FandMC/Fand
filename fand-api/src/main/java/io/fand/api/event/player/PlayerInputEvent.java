package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.entity.PlayerInput;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a client input packet is applied.
 * Cancelling preserves {@link #previousInput()}; listeners may otherwise
 * replace the input that is stored by the server.
 */
public final class PlayerInputEvent implements Event, Cancellable {

    private final Player player;
    private final PlayerInput previousInput;
    private PlayerInput input;
    private boolean cancelled;

    public PlayerInputEvent(Player player, PlayerInput previousInput, PlayerInput input) {
        this.player = Objects.requireNonNull(player, "player");
        this.previousInput = Objects.requireNonNull(previousInput, "previousInput");
        this.input = Objects.requireNonNull(input, "input");
    }

    public Player player() {
        return player;
    }

    public PlayerInput previousInput() {
        return previousInput;
    }

    public PlayerInput input() {
        return input;
    }

    public void setInput(PlayerInput input) {
        this.input = Objects.requireNonNull(input, "input");
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
