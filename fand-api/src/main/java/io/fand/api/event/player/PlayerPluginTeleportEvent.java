package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.world.Location;

/**
 * Fired on the server thread before plugin code teleports a player.
 */
public final class PlayerPluginTeleportEvent extends PlayerTeleportEvent {

    public PlayerPluginTeleportEvent(Player player, Location from, Location to) {
        super(player, from, to, Cause.PLUGIN);
    }
}
