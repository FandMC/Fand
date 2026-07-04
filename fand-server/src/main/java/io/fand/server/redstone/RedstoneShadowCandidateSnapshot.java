package io.fand.server.redstone;

public record RedstoneShadowCandidateSnapshot(
        String level,
        int minChunkX,
        int minChunkZ,
        int maxChunkX,
        int maxChunkZ,
        boolean ready,
        long stableObservations,
        long blockedObservations,
        long lastSamples,
        long lastNanos,
        String lastReason
) {
}
