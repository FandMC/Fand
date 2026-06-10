package io.fand.server.chunk;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable view-change snapshot handed to the async diff worker.
 *
 * <p>Ownership transfer: callers pass freshly built chunk lists and must not
 * mutate them afterwards. The constructor wraps them unmodifiable without
 * copying — the patched {@code ChunkMap} call site allocates new lists per
 * submission, and copying here doubled the per-move allocation cost at view
 * distance 32 (~4200 longs per list).
 */
public record ChunkTrackingSnapshot(
        UUID playerId,
        long sequence,
        long previousCenter,
        int previousViewDistance,
        boolean previousPositioned,
        long nextCenter,
        int nextViewDistance,
        boolean nextPositioned,
        LongList previousChunks,
        LongList nextChunks
) {
    public ChunkTrackingSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        previousChunks = LongLists.unmodifiable(Objects.requireNonNull(previousChunks, "previousChunks"));
        nextChunks = LongLists.unmodifiable(Objects.requireNonNull(nextChunks, "nextChunks"));
    }

    public static ChunkTrackingSnapshot emptyToPositioned(
            UUID playerId,
            long sequence,
            long nextCenter,
            int nextViewDistance,
            LongList nextChunks
    ) {
        return new ChunkTrackingSnapshot(
                playerId,
                sequence,
                0L,
                0,
                false,
                nextCenter,
                nextViewDistance,
                true,
                LongList.of(),
                nextChunks
        );
    }

    public boolean sameTarget(long center, int viewDistance) {
        return nextPositioned && nextCenter == center && nextViewDistance == viewDistance;
    }
}
