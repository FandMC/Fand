package io.fand.server.chunk;

import io.fand.server.config.FandConfig;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChunkSendScheduler implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkSendScheduler.class);
    private static final int DEFAULT_MAX_AUTO_THREADS = 4;

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

    private volatile ExecutorService worker;
    private volatile int applyBudget;

    public ChunkSendScheduler(FandConfig.Chunks config) {
        Objects.requireNonNull(config, "config");
        this.worker = createWorker(config.workerThreads);
        this.applyBudget = config.trackingDiffApplyBudget;
    }

    public boolean submitTrackingDiff(Object levelId, ChunkTrackingSnapshot snapshot) {
        Objects.requireNonNull(levelId, "levelId");
        Objects.requireNonNull(snapshot, "snapshot");
        if (closed.get()) {
            return false;
        }
        submittedJobs.incrementAndGet();
        try {
            CompletableFuture
                    .supplyAsync(() -> compute(snapshot), worker)
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
            } else {
                staleJobs.incrementAndGet();
            }
            applied++;
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
        var replacement = createWorker(config.workerThreads);
        var previous = worker;
        worker = replacement;
        previous.shutdown();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        worker.shutdownNow();
        completedByLevel.clear();
    }

    private long completedQueueSize() {
        return completedByLevel.values().stream().mapToLong(ConcurrentLinkedQueue::size).sum();
    }

    private static ChunkTrackingDiff compute(ChunkTrackingSnapshot snapshot) {
        return ChunkTrackingDiffs.compute(snapshot);
    }

    private static ExecutorService createWorker(int configuredThreads) {
        return Executors.newFixedThreadPool(workerThreadCount(configuredThreads), threadFactory());
    }

    static int workerThreadCount(int configuredThreads) {
        if (configuredThreads < 0) {
            throw new IllegalArgumentException("configuredThreads must not be negative");
        }
        if (configuredThreads > 0) {
            return configuredThreads;
        }
        return Math.max(1, Math.min(DEFAULT_MAX_AUTO_THREADS, Runtime.getRuntime().availableProcessors() / 2));
    }

    private static ThreadFactory threadFactory() {
        var threadId = new AtomicLong();
        return task -> {
            var thread = new Thread(task, "Fand Chunk Worker-" + threadId.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @FunctionalInterface
    public interface TrackingDiffApplier {
        boolean apply(ChunkTrackingDiff diff);
    }
}
