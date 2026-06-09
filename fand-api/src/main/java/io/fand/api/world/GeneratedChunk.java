package io.fand.api.world;

import io.fand.api.block.BlockType;
import java.util.Objects;

/**
 * Mutable chunk-generation target passed to custom world generators.
 *
 * <p>The coordinates are absolute block coordinates. Implementations may reject
 * writes outside the chunk currently being generated.
 */
public interface GeneratedChunk {

    int chunkX();

    int chunkZ();

    int minY();

    int maxY();

    default void setBlock(int x, int y, int z, BlockType type) {
        setBlock(x, y, z, type, BlockUpdateMode.SILENT);
    }

    void setBlock(int x, int y, int z, BlockType type, BlockUpdateMode updateMode);

    default void fill(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockType type) {
        fill(minX, minY, minZ, maxX, maxY, maxZ, type, BlockUpdateMode.SILENT);
    }

    default void fill(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            BlockType type,
            BlockUpdateMode updateMode
    ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(updateMode, "updateMode");
        int fromX = Math.min(minX, maxX);
        int fromY = Math.min(minY, maxY);
        int fromZ = Math.min(minZ, maxZ);
        int toX = Math.max(minX, maxX);
        int toY = Math.max(minY, maxY);
        int toZ = Math.max(minZ, maxZ);
        for (int y = fromY; y <= toY; y++) {
            for (int z = fromZ; z <= toZ; z++) {
                for (int x = fromX; x <= toX; x++) {
                    setBlock(x, y, z, type, updateMode);
                }
            }
        }
    }
}
