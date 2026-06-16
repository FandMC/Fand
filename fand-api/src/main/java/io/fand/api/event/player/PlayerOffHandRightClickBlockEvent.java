package io.fand.api.event.player;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;

/**
 * Fired on the server thread before a player right-clicks a block with their off hand.
 */
public final class PlayerOffHandRightClickBlockEvent extends PlayerRightClickBlockEvent {

    public PlayerOffHandRightClickBlockEvent(Player player, Block block, ItemStack item) {
        super(player, Hand.OFF_HAND, block, item);
    }
}
