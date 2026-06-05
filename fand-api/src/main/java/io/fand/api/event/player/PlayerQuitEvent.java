package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the main thread after a player disconnects but before their handle
 * is invalidated, allowing listeners to read final state.
 *
 * <p>Listeners may replace the broadcast quit message; setting it to {@code null}
 * suppresses the broadcast entirely.
 */
public final class PlayerQuitEvent implements Event {

    private final Player player;
    private @Nullable Component message;

    public PlayerQuitEvent(Player player, @Nullable Component message) {
        this.player = player;
        this.message = message;
    }

    public Player player() {
        return player;
    }

    public @Nullable Component message() {
        return message;
    }

    public void setMessage(@Nullable Component message) {
        this.message = message;
    }
}
