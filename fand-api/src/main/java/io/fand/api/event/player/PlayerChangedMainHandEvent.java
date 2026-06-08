package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread after a client reports a different dominant hand.
 */
public final class PlayerChangedMainHandEvent implements Event {

    private final Player player;
    private final MainHand oldMainHand;
    private final MainHand newMainHand;

    public PlayerChangedMainHandEvent(Player player, MainHand oldMainHand, MainHand newMainHand) {
        this.player = Objects.requireNonNull(player, "player");
        this.oldMainHand = Objects.requireNonNull(oldMainHand, "oldMainHand");
        this.newMainHand = Objects.requireNonNull(newMainHand, "newMainHand");
    }

    public Player player() {
        return player;
    }

    public MainHand oldMainHand() {
        return oldMainHand;
    }

    public MainHand newMainHand() {
        return newMainHand;
    }

    public enum MainHand {
        LEFT,
        RIGHT
    }
}
