package io.fand.api.world;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * In-memory relative block clipboard for paste operations.
 */
public record BlockClipboard(int sizeX, int sizeY, int sizeZ, List<BlockBatchChange> blocks) {

    public BlockClipboard {
        if (sizeX < 0 || sizeY < 0 || sizeZ < 0) {
            throw new IllegalArgumentException("clipboard sizes must not be negative");
        }
        Objects.requireNonNull(blocks, "blocks");
        var copied = List.copyOf(blocks);
        for (var block : copied) {
            Objects.requireNonNull(block, "block");
            if (block.x() < 0 || block.y() < 0 || block.z() < 0
                    || block.x() >= sizeX || block.y() >= sizeY || block.z() >= sizeZ) {
                throw new IllegalArgumentException("clipboard block is outside declared size: "
                        + block.x() + "," + block.y() + "," + block.z());
            }
        }
        blocks = copied;
    }

    public static BlockClipboard of(int sizeX, int sizeY, int sizeZ, Collection<BlockBatchChange> blocks) {
        return new BlockClipboard(sizeX, sizeY, sizeZ, List.copyOf(Objects.requireNonNull(blocks, "blocks")));
    }

    public boolean empty() {
        return blocks.isEmpty();
    }
}
