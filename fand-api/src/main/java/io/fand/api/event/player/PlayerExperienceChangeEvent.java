package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before experience points are added to or removed from a player.
 */
public final class PlayerExperienceChangeEvent implements Event, Cancellable {

    private final Player player;
    private int amount;
    private boolean cancelled;

    public PlayerExperienceChangeEvent(Player player, int amount) {
        this.player = Objects.requireNonNull(player, "player");
        this.amount = amount;
    }

    public Player player() {
        return player;
    }

    public int amount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
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
