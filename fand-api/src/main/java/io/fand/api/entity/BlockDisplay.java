package io.fand.api.entity;

import io.fand.api.block.BlockType;
import java.util.Map;

/**
 * Block display entity.
 */
public interface BlockDisplay extends Display {

    BlockType displayedBlock();

    Map<String, String> displayedBlockStateProperties();

    boolean setDisplayedBlock(BlockType type);

    boolean setDisplayedBlock(BlockType type, Map<String, String> properties);
}
