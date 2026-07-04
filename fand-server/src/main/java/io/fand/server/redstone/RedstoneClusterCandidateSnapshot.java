package io.fand.server.redstone;

public record RedstoneClusterCandidateSnapshot(
        String level,
        int minChunkX,
        int minChunkZ,
        int maxChunkX,
        int maxChunkZ,
        int chunks,
        int coveredRegions,
        int dirtyRegions,
        boolean hot,
        boolean readyForShadow,
        long samples,
        long totalNanos,
        long activityCount,
        long invalidationCount,
        String blockedReason
) {
}
