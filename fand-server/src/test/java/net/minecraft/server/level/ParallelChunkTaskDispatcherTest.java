package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ParallelChunkTaskDispatcherTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void schedulesSameLaneChunksConcurrently() throws Exception {
        ExecutorService worldgenExecutor = Executors.newFixedThreadPool(2);
        try (
                var lanes = new RegionizedChunkTaskScheduler(2, "test");
                var dispatcher = new ParallelChunkTaskDispatcher(
                    TaskScheduler.wrapExecutor("test-worldgen", worldgenExecutor),
                    Runnable::run,
                    2,
                    lanes
                )
        ) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);
            var sameLaneStarted = new CountDownLatch(1);

            dispatcher.submit(() -> {
                firstStarted.countDown();
                await(releaseFirst);
            }, ChunkPos.pack(0, 0), () -> 1);
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            dispatcher.submit(sameLaneStarted::countDown, ChunkPos.pack(7, 7), () -> 1);

            assertThat(sameLaneStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            releaseFirst.countDown();
        } finally {
            worldgenExecutor.shutdownNow();
        }
    }

    @Test
    void schedulesNearbyChunksConcurrentlyBecauseStepSchedulerOwnsWriteLocks() throws Exception {
        ExecutorService worldgenExecutor = Executors.newFixedThreadPool(2);
        try (
                var lanes = new RegionizedChunkTaskScheduler(2, "test");
                var dispatcher = new ParallelChunkTaskDispatcher(
                    TaskScheduler.wrapExecutor("test-worldgen", worldgenExecutor),
                    Runnable::run,
                    2,
                    lanes
                )
        ) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);
            var nearbyStarted = new CountDownLatch(1);

            dispatcher.submit(() -> {
                firstStarted.countDown();
                await(releaseFirst);
            }, ChunkPos.pack(0, 0), () -> 1);
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            dispatcher.submit(nearbyStarted::countDown, ChunkPos.pack(4, 4), () -> 1);

            assertThat(nearbyStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            releaseFirst.countDown();
        } finally {
            worldgenExecutor.shutdownNow();
        }
    }

    @Test
    void schedulesSameChunkBatchConcurrentlyBecauseHolderOwnsStatusClaims() throws Exception {
        ExecutorService worldgenExecutor = Executors.newFixedThreadPool(2);
        try (
                var lanes = new RegionizedChunkTaskScheduler(2, "test");
                var dispatcher = new ParallelChunkTaskDispatcher(
                    TaskScheduler.wrapExecutor("test-worldgen", worldgenExecutor),
                    Runnable::run,
                    2,
                    lanes
                )
        ) {
            long chunk = ChunkPos.pack(0, 0);
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);
            var secondStarted = new CountDownLatch(1);

            dispatcher.submit(() -> {
                firstStarted.countDown();
                await(releaseFirst);
            }, chunk, () -> 1);
            dispatcher.submit(secondStarted::countDown, chunk, () -> 1);

            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();
            assertThat(secondStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            releaseFirst.countDown();
        } finally {
            worldgenExecutor.shutdownNow();
        }
    }

    @Test
    void preservesPriorityAcrossAvailableLanes() throws Exception {
        ExecutorService worldgenExecutor = Executors.newFixedThreadPool(2);
        try (
                var lanes = new RegionizedChunkTaskScheduler(3, "test");
                var dispatcher = new ParallelChunkTaskDispatcher(
                    TaskScheduler.wrapExecutor("test-worldgen", worldgenExecutor),
                    Runnable::run,
                    1,
                    lanes
                )
        ) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);
            var highPriorityStarted = new CountDownLatch(1);
            var releaseHighPriority = new CountDownLatch(1);
            var lowPriorityStarted = new CountDownLatch(1);

            dispatcher.submit(() -> {
                firstStarted.countDown();
                await(releaseFirst);
            }, ChunkPos.pack(0, 0), () -> 1);
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            dispatcher.submit(lowPriorityStarted::countDown, ChunkPos.pack(RegionizedChunkTaskScheduler.REGION_SIZE_CHUNKS, 0), () -> 2);
            dispatcher.submit(() -> {
                highPriorityStarted.countDown();
                await(releaseHighPriority);
            }, ChunkPos.pack(RegionizedChunkTaskScheduler.REGION_SIZE_CHUNKS * 2, 0), () -> 1);

            releaseFirst.countDown();

            assertThat(highPriorityStarted.await(5L, TimeUnit.SECONDS)).isTrue();
            assertThat(lowPriorityStarted.await(100L, TimeUnit.MILLISECONDS)).isFalse();

            releaseHighPriority.countDown();
            assertThat(lowPriorityStarted.await(5L, TimeUnit.SECONDS)).isTrue();
        } finally {
            worldgenExecutor.shutdownNow();
        }
    }

    @Test
    void wakesImmediatelyForNewWorkWhileAnotherBatchIsRunning() throws Exception {
        ExecutorService worldgenExecutor = Executors.newFixedThreadPool(2);
        try (
                var lanes = new RegionizedChunkTaskScheduler(2, "test");
                var dispatcher = new ParallelChunkTaskDispatcher(
                    TaskScheduler.wrapExecutor("test-worldgen", worldgenExecutor),
                    Runnable::run,
                    2,
                    lanes
                )
        ) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);
            var secondStarted = new CountDownLatch(1);

            dispatcher.submit(() -> {
                firstStarted.countDown();
                await(releaseFirst);
            }, ChunkPos.pack(0, 0), () -> 1);
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            dispatcher.submit(secondStarted::countDown, ChunkPos.pack(RegionizedChunkTaskScheduler.REGION_SIZE_CHUNKS, 0), () -> 1);

            assertThat(secondStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            releaseFirst.countDown();
        } finally {
            worldgenExecutor.shutdownNow();
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
