package io.fand.server.redstone;

import net.minecraft.core.BlockPos;

public record RedstoneRegionKey(String level, int regionX, int regionZ) {

    public static final int REGION_SIZE_CHUNKS = 8;

    public static RedstoneRegionKey fromBlock(String level, long blockPos) {
        return fromChunk(level, BlockPos.getX(blockPos) >> 4, BlockPos.getZ(blockPos) >> 4);
    }

    public static RedstoneRegionKey fromChunk(String level, int chunkX, int chunkZ) {
        return new RedstoneRegionKey(
                normalizeLevel(level),
                Math.floorDiv(chunkX, REGION_SIZE_CHUNKS),
                Math.floorDiv(chunkZ, REGION_SIZE_CHUNKS));
    }

    public int minChunkX() {
        return regionX * REGION_SIZE_CHUNKS;
    }

    public int minChunkZ() {
        return regionZ * REGION_SIZE_CHUNKS;
    }

    public int maxChunkX() {
        return minChunkX() + REGION_SIZE_CHUNKS - 1;
    }

    public int maxChunkZ() {
        return minChunkZ() + REGION_SIZE_CHUNKS - 1;
    }

    static String normalizeLevel(String level) {
        return level == null ? "unknown" : level;
    }
}
