package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;

/**
 * Fired on the server thread before a player right-clicks empty space with their off hand.
 */
public final class PlayerOffHandRightClickAirEvent extends PlayerRightClickAirEvent {

    public PlayerOffHandRightClickAirEvent(Player player, ItemStack item) {
        super(player, Hand.OFF_HAND, item);
    }
}
