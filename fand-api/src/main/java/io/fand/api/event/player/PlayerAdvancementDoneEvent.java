package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Fired on the server thread after a player completes an advancement.
 */
public final class PlayerAdvancementDoneEvent implements Event {

    private final Player player;
    private final Key advancement;

    public PlayerAdvancementDoneEvent(Player player, Key advancement) {
        this.player = Objects.requireNonNull(player, "player");
        this.advancement = Objects.requireNonNull(advancement, "advancement");
    }

    public Player player() {
        return player;
    }

    public Key advancement() {
        return advancement;
    }
}
