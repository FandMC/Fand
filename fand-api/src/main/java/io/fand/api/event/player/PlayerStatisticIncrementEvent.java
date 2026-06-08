package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Fired before a player statistic is incremented.
 */
public final class PlayerStatisticIncrementEvent implements Event, Cancellable {

    private final Player player;
    private final Key statistic;
    private final int previousValue;
    private int newValue;
    private boolean cancelled;

    public PlayerStatisticIncrementEvent(Player player, Key statistic, int previousValue, int newValue) {
        this.player = Objects.requireNonNull(player, "player");
        this.statistic = Objects.requireNonNull(statistic, "statistic");
        this.previousValue = previousValue;
        this.newValue = newValue;
    }

    public Player player() {
        return player;
    }

    public Key statistic() {
        return statistic;
    }

    public int previousValue() {
        return previousValue;
    }

    public int newValue() {
        return newValue;
    }

    public void setNewValue(int newValue) {
        this.newValue = Math.max(0, newValue);
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
