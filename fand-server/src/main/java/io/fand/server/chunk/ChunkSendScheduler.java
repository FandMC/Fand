package io.fand.server.chunk;

import io.fand.server.config.FandConfig;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChunkSendScheduler implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkSendScheduler.class);

    // Keyed by an opaque per-level identity (the patched caller passes the
    // level's dimension ResourceKey, which is identity-stable for the lifetime
    // of the level). Using the caller-supplied key directly keeps this class
    // free of vanilla imports and avoids per-tick key allocation.
    private final Map<Object, ConcurrentLinkedQueue<ChunkTrackingDiff>> completedByLevel = new ConcurrentHashMap<>();
    private final AtomicLong submittedJobs = new AtomicLong();
    private final AtomicLong completedJobs = new AtomicLong();
    private final AtomicLong appliedJobs = new AtomicLong();
    private final AtomicLong staleJobs = new AtomicLong();
    private final AtomicLong failedJobs = new AtomicLong();
    private final AtomicLong enteredChunks = new AtomicLong();
    private final AtomicLong leftChunks = new AtomicLong();
    private final AtomicLong totalWorkerNanos = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final RegionTaskScheduler worker;
    private volatile int applyBudget;
    private volatile int dynamicBudget;

    public ChunkSendScheduler(FandConfig.Chunks config) {
        Objects.requireNonNull(config, "config");
        this.worker = new RegionTaskScheduler(config.workerThreads);
        this.applyBudget = config.trackingDiffApplyBudget;
        this.dynamicBudget = config.trackingDiffApplyBudget;
    }

    public boolean submitTrackingDiff(Object levelId, ChunkTrackingSnapshot snapshot) {
        Objects.requireNonNull(levelId, "levelId");
        Objects.requireNonNull(snapshot, "snapshot");
        if (closed.get()) {
            return false;
        }
        submittedJobs.incrementAndGet();
        try {
            worker.submit(levelId, schedulingCenter(snapshot), () -> compute(snapshot))
                    .whenComplete((diff, failure) -> {
                        if (failure != null) {
                            failedJobs.incrementAndGet();
                            LOGGER.warn("Chunk tracking diff worker failed", failure);
                            return;
                        }
                        if (closed.get()) {
                            return;
                        }
                        completedByLevel.computeIfAbsent(levelId, ignored -> new ConcurrentLinkedQueue<>()).add(diff);
                        completedJobs.incrementAndGet();
                        totalWorkerNanos.addAndGet(diff.workerNanos());
                    });
            return true;
        } catch (RejectedExecutionException rejected) {
            failedJobs.incrementAndGet();
            return false;
        }
    }

    public int applyCompleted(Object levelId, TrackingDiffApplier applier) {
        Objects.requireNonNull(levelId, "levelId");
        Objects.requireNonNull(applier, "applier");
        int applied = 0;
        int budget = applyBudget;
        if (budget > 0) {
            var queueSize = completedQueueSize(levelId);
            if (queueSize > budget * 2) {
                dynamicBudget = Math.min(budget * 2, (int) (queueSize * 0.5));
            } else {
                dynamicBudget = budget;
            }
            budget = dynamicBudget;
        }
        var completed = completedByLevel.get(levelId);
        if (completed == null) {
            return 0;
        }
        while (budget <= 0 || applied < budget) {
            var diff = completed.poll();
            if (diff == null) {
                break;
            }
            if (applier.apply(diff)) {
                appliedJobs.incrementAndGet();
                enteredChunks.addAndGet(diff.enter().size());
                leftChunks.addAndGet(diff.leave().size());
                applied++;
            } else {
                staleJobs.incrementAndGet();
            }
        }
        return applied;
    }

    public ChunkTrackingMetrics metrics() {
        return new ChunkTrackingMetrics(
                submittedJobs.get(),
                completedJobs.get(),
                appliedJobs.get(),
                staleJobs.get(),
                failedJobs.get(),
                Math.max(0L, submittedJobs.get() - completedJobs.get() - failedJobs.get()) + completedQueueSize(),
                enteredChunks.get(),
                leftChunks.get(),
                totalWorkerNanos.get()
        );
    }

    public void reconfigure(FandConfig.Chunks config) {
        Objects.requireNonNull(config, "config");
        applyBudget = config.trackingDiffApplyBudget;
        dynamicBudget = config.trackingDiffApplyBudget;
        worker.reconfigure(config.workerThreads);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        worker.close();
        completedByLevel.clear();
    }

    private long completedQueueSize() {
        return completedByLevel.values().stream().mapToLong(ConcurrentLinkedQueue::size).sum();
    }

    private long completedQueueSize(Object levelId) {
        var queue = completedByLevel.get(levelId);
        return queue == null ? 0L : queue.size();
    }

    private static ChunkTrackingDiff compute(ChunkTrackingSnapshot snapshot) {
        return ChunkTrackingDiffs.compute(snapshot);
    }

    private static long schedulingCenter(ChunkTrackingSnapshot snapshot) {
        if (snapshot.nextPositioned()) {
            return snapshot.nextCenter();
        }
        if (snapshot.previousPositioned()) {
            return snapshot.previousCenter();
        }
        return 0L;
    }

    @FunctionalInterface
    public interface TrackingDiffApplier {
        boolean apply(ChunkTrackingDiff diff);
    }
}
