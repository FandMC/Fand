package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.entity.Player;
import java.util.List;
import java.util.Objects;

/**
 * Fired before one placement operation writes multiple blocks.
 */
public final class BlockMultiPlaceEvent extends BlockPlaceEvent {

    private final List<Block> blocks;

    public BlockMultiPlaceEvent(
            Player player,
            Block block,
            BlockType placedType,
            BlockType replacedType,
            List<? extends Block> blocks) {
        super(player, block, placedType, replacedType);
        this.blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
    }

    public List<Block> blocks() {
        return blocks;
    }
}
