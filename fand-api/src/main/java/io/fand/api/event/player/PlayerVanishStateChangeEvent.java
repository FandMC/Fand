package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired by a vanish provider after its effective state for a player changes.
 */
public record PlayerVanishStateChangeEvent(
        Player player,
        boolean oldVanished,
        boolean vanished
) implements Event {

    public PlayerVanishStateChangeEvent {
        Objects.requireNonNull(player, "player");
    }
}
