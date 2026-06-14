package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
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
    void skipsChunksWithOverlappingWriteEnvelopes() throws Exception {
        var worker = Executors.newFixedThreadPool(2);
        try (var dispatcher = new ParallelChunkTaskDispatcher(
                TaskScheduler.wrapExecutor("test-worldgen", worker),
                Runnable::run,
                2
        )) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);
            var overlappingStarted = new CountDownLatch(1);
            var nonOverlappingStarted = new CountDownLatch(1);

            dispatcher.submit(() -> {
                firstStarted.countDown();
                await(releaseFirst);
            }, ChunkPos.pack(0, 0), () -> 1);
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            dispatcher.submit(overlappingStarted::countDown, ChunkPos.pack(4, 0), () -> 1);
            dispatcher.submit(nonOverlappingStarted::countDown, ChunkPos.pack(5, 0), () -> 1);

            assertThat(nonOverlappingStarted.await(5L, TimeUnit.SECONDS)).isTrue();
            assertThat(overlappingStarted.await(100L, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirst.countDown();
            assertThat(overlappingStarted.await(5L, TimeUnit.SECONDS)).isTrue();
        } finally {
            worker.shutdownNow();
        }
    }

    @Test
    void maxChunksInExecutionLimitsConcurrentTasks() throws Exception {
        var worker = Executors.newFixedThreadPool(2);
        try (var dispatcher = new ParallelChunkTaskDispatcher(
                TaskScheduler.wrapExecutor("test-worldgen", worker),
                Runnable::run,
                1
        )) {
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);
            var secondStarted = new CountDownLatch(1);

            dispatcher.submit(() -> {
                firstStarted.countDown();
                await(releaseFirst);
            }, ChunkPos.pack(0, 0), () -> 1);
            assertThat(firstStarted.await(5L, TimeUnit.SECONDS)).isTrue();

            dispatcher.submit(secondStarted::countDown, ChunkPos.pack(5, 0), () -> 1);
            assertThat(secondStarted.await(100L, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirst.countDown();
            assertThat(secondStarted.await(5L, TimeUnit.SECONDS)).isTrue();
        } finally {
            worker.shutdownNow();
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
