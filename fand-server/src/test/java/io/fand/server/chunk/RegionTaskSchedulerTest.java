package io.fand.server.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

final class RegionTaskSchedulerTest {

    @Test
    void mapsNegativeChunksWithFloorDivision() {
        assertThat(RegionTaskScheduler.regionKey("minecraft:overworld", chunkPos(-1, -1)))
                .isEqualTo(new RegionTaskScheduler.RegionKey("minecraft:overworld", -1, -1));
        assertThat(RegionTaskScheduler.regionKey("minecraft:overworld", chunkPos(-8, -8)))
                .isEqualTo(new RegionTaskScheduler.RegionKey("minecraft:overworld", -1, -1));
        assertThat(RegionTaskScheduler.regionKey("minecraft:overworld", chunkPos(-9, -9)))
                .isEqualTo(new RegionTaskScheduler.RegionKey("minecraft:overworld", -2, -2));
    }

    @Test
    void serializesTasksInSameRegion() throws Exception {
        try (var scheduler = new RegionTaskScheduler(2)) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);
            var secondCompleted = new CountDownLatch(1);
            var firstReleased = new AtomicBoolean(false);

            var first = scheduler.submit("minecraft:overworld", chunkPos(0, 0), () -> {
                firstStarted.countDown();
                await(releaseFirst);
                firstReleased.set(true);
                return true;
            });

            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();
            var second = scheduler.submit("minecraft:overworld", chunkPos(7, 7), () -> {
                secondCompleted.countDown();
                return firstReleased.get();
            });

            assertThat(secondCompleted.await(100L, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirst.countDown();
            assertThat(first.get(5L, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(5L, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void runsRegionsMappedToDifferentWorkersConcurrently() throws Exception {
        var levelId = "minecraft:overworld";
        var workerCount = 2;
        var blockedRegion = chunkPos(0, 0);
        var parallelRegion = chunkInDifferentWorker(levelId, blockedRegion, workerCount);

        try (var scheduler = new RegionTaskScheduler(workerCount)) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);

            var first = scheduler.submit(levelId, blockedRegion, () -> {
                firstStarted.countDown();
                await(releaseFirst);
                return true;
            });

            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();
            var second = scheduler.submit(levelId, parallelRegion, () -> true);

            assertThat(second.get(5L, TimeUnit.SECONDS)).isTrue();
            assertThat(first.isDone()).isFalse();

            releaseFirst.countDown();
            assertThat(first.get(5L, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static long chunkPos(int x, int z) {
        return ((long) z << 32) | (x & 0xffffffffL);
    }

    private static long chunkInDifferentWorker(Object levelId, long chunk, int workerCount) {
        var blockedWorker = workerIndex(levelId, chunk, workerCount);
        for (int regionX = -16; regionX <= 16; regionX++) {
            for (int regionZ = -16; regionZ <= 16; regionZ++) {
                var candidate = chunkPos(
                        regionX * RegionTaskScheduler.REGION_SIZE_CHUNKS,
                        regionZ * RegionTaskScheduler.REGION_SIZE_CHUNKS
                );
                if (workerIndex(levelId, candidate, workerCount) != blockedWorker) {
                    return candidate;
                }
            }
        }
        throw new AssertionError("could not find a region mapped to a different worker");
    }

    private static int workerIndex(Object levelId, long chunk, int workerCount) {
        return Math.floorMod(RegionTaskScheduler.regionKey(levelId, chunk).hashCode(), workerCount);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5L, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }
}
