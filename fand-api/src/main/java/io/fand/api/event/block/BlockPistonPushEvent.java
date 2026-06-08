package io.fand.api.event.block;

import io.fand.api.block.Block;
import java.util.List;

/**
 * Fired on the server thread before a piston extends and pushes blocks.
 *
 * <p>This is the push-named form of {@link BlockPistonExtendEvent}; listeners
 * registered for {@code BlockPistonExtendEvent} also receive this event.
 */
public final class BlockPistonPushEvent extends BlockPistonExtendEvent {

    public BlockPistonPushEvent(Block block, BlockFace direction, List<Block> affectedBlocks) {
        super(block, direction, affectedBlocks);
    }
}
