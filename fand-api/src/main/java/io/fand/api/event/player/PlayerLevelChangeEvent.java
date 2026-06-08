package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread after a player's experience level changes.
 */
public final class PlayerLevelChangeEvent implements Event {

    private final Player player;
    private final int oldLevel;
    private final int newLevel;

    public PlayerLevelChangeEvent(Player player, int oldLevel, int newLevel) {
        this.player = Objects.requireNonNull(player, "player");
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Player player() {
        return player;
    }

    public int oldLevel() {
        return oldLevel;
    }

    public int newLevel() {
        return newLevel;
    }
}
