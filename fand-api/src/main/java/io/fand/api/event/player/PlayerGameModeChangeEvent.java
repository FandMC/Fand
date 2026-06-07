package io.fand.api.event.player;

import io.fand.api.entity.GameMode;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a player's game mode changes.
 */
public final class PlayerGameModeChangeEvent implements Event, Cancellable {

    private final Player player;
    private final GameMode fromGameMode;
    private GameMode toGameMode;
    private boolean cancelled;

    public PlayerGameModeChangeEvent(Player player, GameMode fromGameMode, GameMode toGameMode) {
        this.player = Objects.requireNonNull(player, "player");
        this.fromGameMode = Objects.requireNonNull(fromGameMode, "fromGameMode");
        this.toGameMode = Objects.requireNonNull(toGameMode, "toGameMode");
    }

    public Player player() {
        return player;
    }

    public GameMode fromGameMode() {
        return fromGameMode;
    }

    public GameMode toGameMode() {
        return toGameMode;
    }

    public void setToGameMode(GameMode toGameMode) {
        this.toGameMode = Objects.requireNonNull(toGameMode, "toGameMode");
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
