package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.world.Location;

/**
 * Fired on the server thread before an ender pearl teleports a player.
 */
public final class PlayerEnderPearlTeleportEvent extends PlayerTeleportEvent {

    public PlayerEnderPearlTeleportEvent(Player player, Location from, Location to) {
        super(player, from, to, Cause.ENDER_PEARL);
    }
}
