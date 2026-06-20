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
        // Wrap the supplied list unmodifiable without copying when it is already a
        // LongArrayList (the only producer hands one straight over); otherwise take
        // a defensive copy. Avoids the previous double-allocation that re-copied an
        // already-built list on every view-distance change.
        enter = wrapUnmodifiable(enter);
        leave = wrapUnmodifiable(leave);
        if (workerNanos < 0L) {
            throw new IllegalArgumentException("workerNanos must be >= 0");
        }
    }

    private static LongList wrapUnmodifiable(LongList list) {
        if (list instanceof LongArrayList direct) {
            return LongLists.unmodifiable(direct);
        }
        return LongLists.unmodifiable(new LongArrayList(list));
    }
}
