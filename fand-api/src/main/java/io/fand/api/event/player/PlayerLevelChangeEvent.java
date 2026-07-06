package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread after a player's experience level changes.
 */
public record PlayerLevelChangeEvent(Player player, int oldLevel, int newLevel) implements Event {

    public PlayerLevelChangeEvent(Player player, int oldLevel, int newLevel) {
        this.player = Objects.requireNonNull(player, "player");
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }
}
