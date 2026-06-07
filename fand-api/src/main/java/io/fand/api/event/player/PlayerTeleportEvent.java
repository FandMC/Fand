package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread before a player is teleported. Cancelling the
 * event rejects the teleport. Listeners may replace the destination.
 */
public final class PlayerTeleportEvent implements Event, Cancellable {

    public enum Cause {
        COMMAND,
        PLUGIN,
        ENDER_PEARL,
        PORTAL,
        SPECTATE,
        UNKNOWN
    }

    private final Player player;
    private final Location from;
    private final Cause cause;
    private Location to;
    private boolean cancelled;

    public PlayerTeleportEvent(Player player, Location from, Location to) {
        this(player, from, to, Cause.UNKNOWN);
    }

    public PlayerTeleportEvent(Player player, Location from, Location to, Cause cause) {
        this.player = Objects.requireNonNull(player, "player");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public Player player() {
        return player;
    }

    public Location from() {
        return from;
    }

    public Location to() {
        return to;
    }

    public void setTo(Location to) {
        this.to = Objects.requireNonNull(to, "to");
    }

    public Cause cause() {
        return cause;
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
