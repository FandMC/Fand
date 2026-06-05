package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread when a player dies, before the death message is
 * broadcast and before the player is sent the respawn screen. The death
 * message can be replaced or suppressed (set to {@code null}) by listeners.
 *
 * <p>Death itself is not cancellable: vanilla has no concept of un-dying once
 * the death sequence has been entered.
 */
public final class PlayerDeathEvent implements Event {

    private final Player player;
    private @Nullable Component deathMessage;

    public PlayerDeathEvent(Player player, @Nullable Component deathMessage) {
        this.player = Objects.requireNonNull(player, "player");
        this.deathMessage = deathMessage;
    }

    public Player player() {
        return player;
    }

    /**
     * Message that will be broadcast for this death. {@code null} suppresses
     * the broadcast entirely.
     */
    public @Nullable Component deathMessage() {
        return deathMessage;
    }

    public void setDeathMessage(@Nullable Component deathMessage) {
        this.deathMessage = deathMessage;
    }
}
