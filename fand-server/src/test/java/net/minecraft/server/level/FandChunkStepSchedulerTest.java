package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class FandChunkStepSchedulerTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void runsOverlappingRadiusZeroStepsConcurrently() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (var scheduler = new FandChunkStepScheduler(executor, 2)) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CompletableFuture<Void>();
            var secondStarted = new CountDownLatch(1);

            scheduler.schedule(new ChunkPos(0, 0), 0, () -> {
                firstStarted.countDown();
                return releaseFirst;
            });
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            CompletableFuture<Void> second = scheduler.schedule(new ChunkPos(1, 0), 0, () -> {
                secondStarted.countDown();
                return CompletableFuture.completedFuture(null);
            });

            assertThat(secondStarted.await(5L, TimeUnit.SECONDS)).isTrue();
            second.get(5L, TimeUnit.SECONDS);
            releaseFirst.complete(null);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void waitsForOverlappingWriteRadius() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (var scheduler = new FandChunkStepScheduler(executor, 2)) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CompletableFuture<Void>();
            var secondStarted = new CountDownLatch(1);

            scheduler.schedule(new ChunkPos(0, 0), 1, () -> {
                firstStarted.countDown();
                return releaseFirst;
            });
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            scheduler.schedule(new ChunkPos(1, 0), 1, () -> {
                secondStarted.countDown();
                return CompletableFuture.completedFuture(null);
            });

            assertThat(secondStarted.await(100L, TimeUnit.MILLISECONDS)).isFalse();
            releaseFirst.complete(null);
            assertThat(secondStarted.await(5L, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void runsNonOverlappingWriteRadiusConcurrently() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (var scheduler = new FandChunkStepScheduler(executor, 2)) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CompletableFuture<Void>();
            var secondStarted = new CountDownLatch(1);

            scheduler.schedule(new ChunkPos(0, 0), 1, () -> {
                firstStarted.countDown();
                return releaseFirst;
            });
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            scheduler.schedule(new ChunkPos(3, 0), 1, () -> {
                secondStarted.countDown();
                return CompletableFuture.completedFuture(null);
            });

            assertThat(secondStarted.await(5L, TimeUnit.SECONDS)).isTrue();
            releaseFirst.complete(null);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void doesNotLetBlockedWideStepPreventIndependentNewStep() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (var scheduler = new FandChunkStepScheduler(executor, 2)) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CompletableFuture<Void>();
            var blockedStarted = new CountDownLatch(1);
            var independentStarted = new CountDownLatch(1);

            scheduler.schedule(new ChunkPos(0, 0), 1, () -> {
                firstStarted.countDown();
                return releaseFirst;
            });
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            scheduler.schedule(new ChunkPos(1, 0), 1, () -> {
                blockedStarted.countDown();
                return CompletableFuture.completedFuture(null);
            });
            assertThat(blockedStarted.await(100L, TimeUnit.MILLISECONDS)).isFalse();

            scheduler.schedule(new ChunkPos(4, 0), 1, () -> {
                independentStarted.countDown();
                return CompletableFuture.completedFuture(null);
            });
            assertThat(independentStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            releaseFirst.complete(null);
            assertThat(blockedStarted.await(5L, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void radiusZeroStepDoesNotWaitForOtherChunkWriteRadius() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (var scheduler = new FandChunkStepScheduler(executor, 2)) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CompletableFuture<Void>();
            var parallelStarted = new CountDownLatch(1);

            scheduler.schedule(new ChunkPos(0, 0), 1, () -> {
                firstStarted.countDown();
                return releaseFirst;
            });
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            scheduler.schedule(new ChunkPos(3, 0), 0, 0, 0, true, () -> {
                parallelStarted.countDown();
                return CompletableFuture.completedFuture(null);
            });

            assertThat(parallelStarted.await(5L, TimeUnit.SECONDS)).isTrue();
            releaseFirst.complete(null);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void radiusZeroStepsForSameChunkDoNotOverlap() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (var scheduler = new FandChunkStepScheduler(executor, 2)) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CompletableFuture<Void>();
            var secondStarted = new CountDownLatch(1);

            scheduler.schedule(new ChunkPos(0, 0), 0, 0, 0, true, () -> {
                firstStarted.countDown();
                return releaseFirst;
            });
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            scheduler.schedule(new ChunkPos(0, 0), 0, 0, 0, true, () -> {
                secondStarted.countDown();
                return CompletableFuture.completedFuture(null);
            });

            assertThat(secondStarted.await(100L, TimeUnit.MILLISECONDS)).isFalse();
            releaseFirst.complete(null);
            assertThat(secondStarted.await(5L, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void queuedStepsRunHigherPriorityFirst() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (var scheduler = new FandChunkStepScheduler(executor, 1)) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CompletableFuture<Void>();
            var releaseHigh = new CompletableFuture<Void>();
            var lowStarted = new CountDownLatch(1);
            var highStarted = new CountDownLatch(1);

            scheduler.schedule(new ChunkPos(0, 0), 1, () -> {
                firstStarted.countDown();
                return releaseFirst;
            });
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            scheduler.schedule(new ChunkPos(4, 0), 0, 0, 20, false, () -> {
                lowStarted.countDown();
                return CompletableFuture.completedFuture(null);
            });
            scheduler.schedule(new ChunkPos(8, 0), 0, 0, 1, false, () -> {
                highStarted.countDown();
                return releaseHigh;
            });

            releaseFirst.complete(null);
            assertThat(highStarted.await(5L, TimeUnit.SECONDS)).isTrue();
            assertThat(lowStarted.await(100L, TimeUnit.MILLISECONDS)).isFalse();
            releaseHigh.complete(null);
            assertThat(lowStarted.await(5L, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

}
