package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread after a player disconnects but before their handle
 * is invalidated, allowing listeners to read final state.
 *
 * <p>Listeners may replace the broadcast quit message; setting it to {@code null}
 * suppresses the broadcast entirely.
 */
public final class PlayerQuitEvent implements Event {

    private final Player player;
    private final DisconnectReason reason;
    private final Component disconnectMessage;
    private @Nullable Component message;

    public PlayerQuitEvent(Player player, @Nullable Component message) {
        this(player, DisconnectReason.UNKNOWN, Component.empty(), message);
    }

    public PlayerQuitEvent(Player player, DisconnectReason reason, Component disconnectMessage, @Nullable Component message) {
        this.player = player;
        this.reason = java.util.Objects.requireNonNull(reason, "reason");
        this.disconnectMessage = java.util.Objects.requireNonNull(disconnectMessage, "disconnectMessage");
        this.message = message;
    }

    public Player player() {
        return player;
    }

    public DisconnectReason reason() {
        return reason;
    }

    public Component disconnectMessage() {
        return disconnectMessage;
    }

    public @Nullable Component message() {
        return message;
    }

    public void setMessage(@Nullable Component message) {
        this.message = message;
    }
}
