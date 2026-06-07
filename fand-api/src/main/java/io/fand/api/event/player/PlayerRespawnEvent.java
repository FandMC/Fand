package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread while a player is being respawned, before the
 * respawn packets are sent. Listeners may replace the respawn location.
 */
public final class PlayerRespawnEvent implements Event {

    public enum Cause {
        DEATH,
        DIMENSION_CHANGE,
        UNKNOWN
    }

    private final Player player;
    private final Cause cause;
    private final boolean keepAllPlayerData;
    private Location respawnLocation;

    public PlayerRespawnEvent(Player player, Location respawnLocation, Cause cause, boolean keepAllPlayerData) {
        this.player = Objects.requireNonNull(player, "player");
        this.respawnLocation = Objects.requireNonNull(respawnLocation, "respawnLocation");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.keepAllPlayerData = keepAllPlayerData;
    }

    public Player player() {
        return player;
    }

    public Location respawnLocation() {
        return respawnLocation;
    }

    public void setRespawnLocation(Location respawnLocation) {
        this.respawnLocation = Objects.requireNonNull(respawnLocation, "respawnLocation");
    }

    public Cause cause() {
        return cause;
    }

    public boolean keepAllPlayerData() {
        return keepAllPlayerData;
    }
}
