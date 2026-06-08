package io.fand.server.chunk;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;

public record ChunkTrackingDiff(
        ChunkTrackingSnapshot snapshot,
        LongList enter,
        LongList leave,
        long workerNanos
) {
    public ChunkTrackingDiff {
        enter = LongLists.unmodifiable(new LongArrayList(enter));
        leave = LongLists.unmodifiable(new LongArrayList(leave));
        if (workerNanos < 0L) {
            throw new IllegalArgumentException("workerNanos must be >= 0");
        }
    }
}
