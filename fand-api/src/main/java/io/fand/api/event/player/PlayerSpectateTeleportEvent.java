package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.world.Location;

/**
 * Fired on the server thread before spectator teleport moves a player.
 */
public final class PlayerSpectateTeleportEvent extends PlayerTeleportEvent {

    public PlayerSpectateTeleportEvent(Player player, Location from, Location to) {
        super(player, from, to, Cause.SPECTATE);
    }
}
