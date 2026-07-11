package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired by a disguise provider after its effective state for a player changes.
 */
public record PlayerDisguiseStateChangeEvent(
        Player player,
        boolean oldDisguised,
        boolean disguised
) implements Event {

    public PlayerDisguiseStateChangeEvent {
        Objects.requireNonNull(player, "player");
    }
}
