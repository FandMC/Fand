package io.fand.api.world;

import io.fand.api.block.BlockType;
import io.fand.api.world.generation.GeneratorContext;
import java.util.Objects;
import net.kyori.adventure.key.Key;

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

    default int minX() {
        return chunkX() << 4;
    }

    default int maxX() {
        return minX() + 15;
    }

    default int minZ() {
        return chunkZ() << 4;
    }

    default int maxZ() {
        return minZ() + 15;
    }

    default GeneratorContext context() {
        return GeneratorContext.unknown();
    }

    default boolean contains(int x, int y, int z) {
        return x >= minX() && x <= maxX()
                && z >= minZ() && z <= maxZ()
                && y >= minY() && y <= maxY();
    }

    BlockType blockType(int x, int y, int z);

    /**
     * Returns the biome stored for the 4x4x4 biome cell containing the block.
     */
    default Key biomeAt(int x, int y, int z) {
        throw new UnsupportedOperationException("Biome reads are not available for this generated chunk");
    }

    /**
     * Sets the biome for the 4x4x4 biome cell containing the block.
     */
    default void setBiome(int x, int y, int z, Key biome) {
        throw new UnsupportedOperationException("Biome writes are not available for this generated chunk");
    }

    default void setBiome(int x, int y, int z, io.fand.api.VanillaKey biome) {
        setBiome(x, y, z, Objects.requireNonNull(biome, "biome").key());
    }

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
