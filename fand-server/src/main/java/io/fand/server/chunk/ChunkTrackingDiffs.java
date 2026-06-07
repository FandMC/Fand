package io.fand.server.chunk;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

final class ChunkTrackingDiffs {

    private ChunkTrackingDiffs() {
    }

    static ChunkTrackingDiff compute(ChunkTrackingSnapshot snapshot) {
        long start = System.nanoTime();
        var enter = new LongArrayList();
        var leave = new LongArrayList();
        var previous = new LongOpenHashSet(snapshot.previousChunks());
        var next = new LongLinkedOpenHashSet(snapshot.nextChunks());
        addMissing(next, previous, enter);
        addMissing(previous, next, leave);
        return new ChunkTrackingDiff(snapshot, enter, leave, System.nanoTime() - start);
    }

    private static void addMissing(LongSet candidates, LongSet existing, LongList output) {
        var iterator = candidates.longIterator();
        while (iterator.hasNext()) {
            long value = iterator.nextLong();
            if (!existing.contains(value)) {
                output.add(value);
            }
        }
    }
}
