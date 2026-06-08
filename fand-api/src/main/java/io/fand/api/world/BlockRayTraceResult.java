package io.fand.api.world;

import io.fand.api.block.Block;
import io.fand.api.event.block.BlockFace;
import java.util.Objects;

/**
 * Result of a world block ray trace.
 */
public record BlockRayTraceResult(Block block, Location hitLocation, BlockFace face, boolean inside) {

    public BlockRayTraceResult {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(hitLocation, "hitLocation");
        Objects.requireNonNull(face, "face");
    }
}
