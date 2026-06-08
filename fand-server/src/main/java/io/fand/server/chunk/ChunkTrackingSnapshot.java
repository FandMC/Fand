package io.fand.server.chunk;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import java.util.Objects;
import java.util.UUID;

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
        previousChunks = immutableCopy(previousChunks, "previousChunks");
        nextChunks = immutableCopy(nextChunks, "nextChunks");
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

    private static LongList immutableCopy(LongList values, String name) {
        Objects.requireNonNull(values, name);
        return LongLists.unmodifiable(new LongArrayList(values));
    }
}
