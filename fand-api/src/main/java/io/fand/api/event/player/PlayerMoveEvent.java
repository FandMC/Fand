package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread for each accepted client movement packet whose
 * resulting position differs from the player's current position. Cancelling
 * the event teleports the player back to {@link #from()} on the same tick,
 * reusing vanilla's anti-cheat snapback path.
 *
 * <p>This event is dispatched on the hot movement path. Listeners should keep
 * work bounded; expensive work belongs on the scheduler.
 */
public final class PlayerMoveEvent implements Event, Cancellable {

    private final Player player;
    private final Location from;
    private final Location to;
    private boolean cancelled;

    public PlayerMoveEvent(Player player, Location from, Location to) {
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

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
