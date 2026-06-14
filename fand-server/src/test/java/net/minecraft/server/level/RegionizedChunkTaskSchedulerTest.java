package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class RegionizedChunkTaskSchedulerTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void serializesTasksInSameRegion() throws Exception {
        try (var scheduler = new RegionizedChunkTaskScheduler(2, "test")) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);
            var secondCompleted = new CountDownLatch(1);
            var firstReleased = new AtomicBoolean(false);

            var first = scheduler.schedule(ChunkPos.pack(0, 0), List.of(() -> {
                firstStarted.countDown();
                await(releaseFirst);
                firstReleased.set(true);
            }));

            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();
            var second = scheduler.schedule(ChunkPos.pack(7, 7), List.of(() -> {
                secondCompleted.countDown();
                assertThat(firstReleased.get()).isTrue();
            }));

            assertThat(secondCompleted.await(100L, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirst.countDown();
            first.get(5L, TimeUnit.SECONDS);
            second.get(5L, TimeUnit.SECONDS);
        }
    }

    @Test
    void runsDifferentLaneRegionsConcurrently() throws Exception {
        var blockedLaneChunk = ChunkPos.pack(0, 0);
        var parallelLaneChunk = ChunkPos.pack(RegionizedChunkTaskScheduler.REGION_SIZE_CHUNKS, 0);
        assertThat(RegionizedChunkTaskScheduler.laneIndex(parallelLaneChunk, 2))
                .isNotEqualTo(RegionizedChunkTaskScheduler.laneIndex(blockedLaneChunk, 2));

        try (var scheduler = new RegionizedChunkTaskScheduler(2, "test")) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);

            var first = scheduler.schedule(blockedLaneChunk, List.of(() -> {
                firstStarted.countDown();
                await(releaseFirst);
            }));

            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();
            var second = scheduler.schedule(parallelLaneChunk, List.of(() -> {
            }));

            second.get(5L, TimeUnit.SECONDS);
            assertThat(first.isDone()).isFalse();

            releaseFirst.countDown();
            first.get(5L, TimeUnit.SECONDS);
        }
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
