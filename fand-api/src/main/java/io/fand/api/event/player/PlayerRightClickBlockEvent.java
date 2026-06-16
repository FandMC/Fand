package io.fand.api.event.player;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a player right-clicks a block.
 */
public class PlayerRightClickBlockEvent extends PlayerInteractEvent {

    private final Block clickedBlock;

    public PlayerRightClickBlockEvent(Player player, Hand hand, Block block, ItemStack item) {
        super(player, Action.RIGHT_CLICK_BLOCK, hand, block, item);
        this.clickedBlock = Objects.requireNonNull(block, "block");
    }

    public Block clickedBlock() {
        return clickedBlock;
    }
}
