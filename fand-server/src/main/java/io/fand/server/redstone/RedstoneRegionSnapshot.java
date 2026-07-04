package io.fand.server.redstone;

public record RedstoneRegionSnapshot(
        String level,
        int regionX,
        int regionZ,
        int minChunkX,
        int minChunkZ,
        int maxChunkX,
        int maxChunkZ,
        boolean hot,
        boolean dirty,
        long generation,
        long samples,
        long totalNanos,
        long activityCount,
        String activityReason,
        long invalidationCount,
        String invalidationReason
) {
}
