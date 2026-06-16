package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.world.Location;

/**
 * Fired on the server thread before an uncategorised teleport moves a player.
 */
public final class PlayerUnknownTeleportEvent extends PlayerTeleportEvent {

    public PlayerUnknownTeleportEvent(Player player, Location from, Location to) {
        super(player, from, to, Cause.UNKNOWN);
    }
}
