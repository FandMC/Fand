package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread after the client confirms that its world is loaded.
 * At this point the client can safely receive world-bound UI and synchronization.
 * The event is fired once for the initial join and again after each respawn load.
 */
public final class PlayerClientLoadedEvent implements Event {

    private final Player player;
    private final boolean initialJoin;

    public PlayerClientLoadedEvent(Player player, boolean initialJoin) {
        this.player = Objects.requireNonNull(player, "player");
        this.initialJoin = initialJoin;
    }

    public Player player() {
        return player;
    }

    public boolean initialJoin() {
        return initialJoin;
    }
}
