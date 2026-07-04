package io.fand.server.redstone;

public record RedstoneWireJitSnapshot(
        int plans,
        long attempts,
        long hits,
        long compiled,
        long guardMisses,
        long capacityMisses,
        long warmupMisses,
        long cooldownMisses,
        long invalidations,
        long blockInvalidations,
        long chunkInvalidations
) {
}
