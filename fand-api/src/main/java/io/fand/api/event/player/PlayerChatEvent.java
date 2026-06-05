package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import net.kyori.adventure.text.Component;

/**
 * Fired on the main thread when a player sends a chat message, before the
 * message is broadcast to recipients.
 *
 * <p>Listeners may replace the broadcast message via {@link #setMessage(Component)}
 * or suppress the broadcast entirely with {@link #setCancelled(boolean) setCancelled(true)}.
 * The original (un-decorated) text the client typed is exposed via {@link #originalText()}
 * and is read-only.
 */
public final class PlayerChatEvent implements Event, Cancellable {

    private final Player player;
    private final String originalText;
    private Component message;
    private boolean cancelled;

    public PlayerChatEvent(Player player, String originalText, Component message) {
        this.player = Objects.requireNonNull(player, "player");
        this.originalText = Objects.requireNonNull(originalText, "originalText");
        this.message = Objects.requireNonNull(message, "message");
    }

    public Player player() {
        return player;
    }

    /** Raw text the client sent, without server-side decoration. */
    public String originalText() {
        return originalText;
    }

    /** Decorated message that will be broadcast unless replaced or cancelled. */
    public Component message() {
        return message;
    }

    public void setMessage(Component message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
