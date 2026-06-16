package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;

/**
 * Fired on the server thread before a player right-clicks empty space.
 */
public class PlayerRightClickAirEvent extends PlayerInteractEvent {

    public PlayerRightClickAirEvent(Player player, Hand hand, ItemStack item) {
        super(player, Action.RIGHT_CLICK_AIR, hand, null, item);
    }
}
