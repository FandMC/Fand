package io.fand.api.event.player;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;

/**
 * Fired on the server thread before a player right-clicks a block with their main hand.
 */
public final class PlayerMainHandRightClickBlockEvent extends PlayerRightClickBlockEvent {

    public PlayerMainHandRightClickBlockEvent(Player player, Block block, ItemStack item) {
        super(player, Hand.MAIN_HAND, block, item);
    }
}
