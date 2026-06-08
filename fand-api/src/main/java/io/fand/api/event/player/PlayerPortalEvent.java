package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread before a player uses a portal.
 *
 * <p>Listeners may cancel the portal transfer or replace the destination.
 */
public final class PlayerPortalEvent implements Event, Cancellable {

    private final Player player;
    private final Location from;
    private Location to;
    private boolean cancelled;

    public PlayerPortalEvent(Player player, Location from, Location to) {
        this.player = Objects.requireNonNull(player, "player");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
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

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
