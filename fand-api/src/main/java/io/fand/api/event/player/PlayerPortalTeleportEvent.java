package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.world.Location;

/**
 * Fired on the server thread before portal travel teleports a player.
 */
public final class PlayerPortalTeleportEvent extends PlayerTeleportEvent {

    public PlayerPortalTeleportEvent(Player player, Location from, Location to) {
        super(player, from, to, Cause.PORTAL);
    }
}
