package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired before the server sends a velocity update to a player.
 */
public final class PlayerVelocityEvent implements Event, Cancellable {

    private final Player player;
    private double x;
    private double y;
    private double z;
    private boolean cancelled;

    public PlayerVelocityEvent(Player player, double x, double y, double z) {
        this.player = Objects.requireNonNull(player, "player");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Player player() {
        return player;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public void setVelocity(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
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
