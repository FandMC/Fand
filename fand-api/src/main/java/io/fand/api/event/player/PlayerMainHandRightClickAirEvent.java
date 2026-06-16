package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;

/**
 * Fired on the server thread before a player right-clicks empty space with their main hand.
 */
public final class PlayerMainHandRightClickAirEvent extends PlayerRightClickAirEvent {

    public PlayerMainHandRightClickAirEvent(Player player, ItemStack item) {
        super(player, Hand.MAIN_HAND, item);
    }
}
