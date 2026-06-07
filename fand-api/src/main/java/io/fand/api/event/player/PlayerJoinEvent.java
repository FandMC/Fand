package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread after a player successfully joins the server.
 *
 * <p>Listeners may replace the broadcast join message; setting it to {@code null}
 * suppresses the broadcast entirely.
 */
public final class PlayerJoinEvent implements Event {

    private final Player player;
    private @Nullable Component message;

    public PlayerJoinEvent(Player player, @Nullable Component message) {
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
