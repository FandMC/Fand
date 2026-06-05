package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a player breaks a block in survival or
 * adventure mode. Cancelling the event leaves the block intact and resyncs
 * the world state to the client.
 */
public final class BlockBreakEvent implements Event, Cancellable {

    private final Player player;
    private final Block block;
    private final BlockType blockType;
    private boolean cancelled;

    public BlockBreakEvent(Player player, Block block, BlockType blockType) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        this.blockType = Objects.requireNonNull(blockType, "blockType");
    }

    public Player player() {
        return player;
    }

    public Block block() {
        return block;
    }

    /** Type of the block at the moment the break was attempted. */
    public BlockType blockType() {
        return blockType;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
