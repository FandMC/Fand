package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread after a client reports a different dominant hand.
 */
public record PlayerChangedMainHandEvent(Player player, MainHand oldMainHand, MainHand newMainHand) implements Event {

    public PlayerChangedMainHandEvent(Player player, MainHand oldMainHand, MainHand newMainHand) {
        this.player = Objects.requireNonNull(player, "player");
        this.oldMainHand = Objects.requireNonNull(oldMainHand, "oldMainHand");
        this.newMainHand = Objects.requireNonNull(newMainHand, "newMainHand");
    }

    public enum MainHand {
        LEFT,
        RIGHT
    }
}
