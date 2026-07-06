package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Fired on the server thread after a player completes an advancement.
 */
public record PlayerAdvancementDoneEvent(Player player, Key advancement) implements Event {

    public PlayerAdvancementDoneEvent(Player player, Key advancement) {
        this.player = Objects.requireNonNull(player, "player");
        this.advancement = Objects.requireNonNull(advancement, "advancement");
    }
}
