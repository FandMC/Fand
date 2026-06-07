package io.fand.server.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.server.config.FandConfig;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class ChunkSendSchedulerTest {

    @Test
    void computesChunkTrackingDiffOffThread() throws Exception {
        var config = new FandConfig.Chunks();
        config.workerThreads = 1;
        config.trackingDiffApplyBudget = 0;
        try (var scheduler = new ChunkSendScheduler(config)) {
            var snapshot = snapshot(LongList.of(1L, 2L, 3L), LongList.of(2L, 3L, 4L));

            assertThat(scheduler.submitTrackingDiff(snapshot)).isTrue();
            awaitCompleted(scheduler, 1);

            var applied = new LongArrayList();
            var removed = new LongArrayList();
            assertThat(scheduler.applyCompleted(diff -> {
                applied.addAll(diff.enter());
                removed.addAll(diff.leave());
                return true;
            })).isEqualTo(1);

            assertThat(applied.toLongArray()).containsExactly(4L);
            assertThat(removed.toLongArray()).containsExactly(1L);
            assertThat(scheduler.metrics().appliedJobs()).isEqualTo(1L);
            assertThat(scheduler.metrics().enteredChunks()).isEqualTo(1L);
            assertThat(scheduler.metrics().leftChunks()).isEqualTo(1L);
        }
    }

    @Test
    void appliesCompletedJobsWithinBudget() throws Exception {
        var config = new FandConfig.Chunks();
        config.workerThreads = 1;
        config.trackingDiffApplyBudget = 1;
        try (var scheduler = new ChunkSendScheduler(config)) {
            scheduler.submitTrackingDiff(snapshot(LongList.of(), LongList.of(1L)));
            scheduler.submitTrackingDiff(snapshot(LongList.of(), LongList.of(2L)));
            awaitCompleted(scheduler, 2);

            assertThat(scheduler.applyCompleted(diff -> true)).isEqualTo(1);
            assertThat(scheduler.metrics().appliedJobs()).isEqualTo(1L);

            assertThat(scheduler.applyCompleted(diff -> true)).isEqualTo(1);
            assertThat(scheduler.metrics().appliedJobs()).isEqualTo(2L);
        }
    }

    @Test
    void countsRejectedDiffAsStale() throws Exception {
        var config = new FandConfig.Chunks();
        config.workerThreads = 1;
        config.trackingDiffApplyBudget = 0;
        try (var scheduler = new ChunkSendScheduler(config)) {
            scheduler.submitTrackingDiff(snapshot(LongList.of(), LongList.of(1L)));
            awaitCompleted(scheduler, 1);

            assertThat(scheduler.applyCompleted(diff -> false)).isEqualTo(1);
            assertThat(scheduler.metrics().staleJobs()).isEqualTo(1L);
            assertThat(scheduler.metrics().appliedJobs()).isZero();
        }
    }

    @Test
    void rejectsSubmissionsAfterClose() {
        var config = new FandConfig.Chunks();
        var scheduler = new ChunkSendScheduler(config);
        scheduler.close();

        assertThat(scheduler.submitTrackingDiff(snapshot(LongList.of(), LongList.of(1L)))).isFalse();
    }

    private static ChunkTrackingSnapshot snapshot(LongList previous, LongList next) {
        return new ChunkTrackingSnapshot(UUID.randomUUID(), 1L, 0L, 2, true, 1L, 2, true, previous, next);
    }

    private static void awaitCompleted(ChunkSendScheduler scheduler, long completed) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5L);
        while (scheduler.metrics().completedJobs() < completed && System.nanoTime() < deadline) {
            Thread.sleep(10L);
        }
        assertThat(scheduler.metrics().completedJobs()).isEqualTo(completed);
    }
}
