package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread after vanilla accepts a player jump from the ground.
 */
public final class PlayerJumpEvent implements Event {

    private final Player player;

    public PlayerJumpEvent(Player player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    public Player player() {
        return player;
    }
}
