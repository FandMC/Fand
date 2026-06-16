package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.world.Location;

/**
 * Fired on the server thread before a command teleports a player.
 */
public final class PlayerCommandTeleportEvent extends PlayerTeleportEvent {

    public PlayerCommandTeleportEvent(Player player, Location from, Location to) {
        super(player, from, to, Cause.COMMAND);
    }
}
