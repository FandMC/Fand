package io.fand.server.redstone;

import java.util.List;

public record RedstoneProbeSnapshot(
        RedstoneJitMode mode,
        long totalCount,
        long totalNanos,
        long droppedPositionSamples,
        List<TypeEntry> types,
        List<ClusterEntry> topClusters,
        List<RegionEntry> topRegions,
        List<ChunkEntry> topChunks,
        List<PositionEntry> topPositions
) {

    public record TypeEntry(RedstoneProbeType type, long count, long totalNanos) {
    }

    public record ClusterEntry(
            String level,
            int minChunkX,
            int minChunkZ,
            int maxChunkX,
            int maxChunkZ,
            int chunks,
            long count,
            long totalNanos
    ) {
    }

    public record RegionEntry(
            String level,
            int regionX,
            int regionZ,
            int minChunkX,
            int minChunkZ,
            int maxChunkX,
            int maxChunkZ,
            long count,
            long totalNanos
    ) {
    }

    public record ChunkEntry(String level, int chunkX, int chunkZ, long count, long totalNanos) {
    }

    public record PositionEntry(RedstoneProbeType type, String level, long blockPos, long count, long totalNanos) {
    }
}
