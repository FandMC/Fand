package io.fand.server.chunk;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

final class RegionTaskScheduler implements AutoCloseable {

    static final int REGION_SIZE_CHUNKS = 8;
    private static final int DEFAULT_MAX_AUTO_THREADS = 4;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile WorkerSet workers;

    RegionTaskScheduler(int configuredThreads) {
        this.workers = WorkerSet.create(configuredThreads);
    }

    <T> CompletableFuture<T> submit(Object levelId, long packedChunk, Supplier<T> task) {
        Objects.requireNonNull(levelId, "levelId");
        Objects.requireNonNull(task, "task");
        if (closed.get()) {
            throw new RejectedExecutionException("region scheduler is closed");
        }
        return workers.submit(regionKey(levelId, packedChunk), task);
    }

    void reconfigure(int configuredThreads) {
        if (closed.get()) {
            throw new RejectedExecutionException("region scheduler is closed");
        }
        var replacement = WorkerSet.create(configuredThreads);
        var previous = workers;
        workers = replacement;
        previous.shutdown();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            workers.shutdownNow();
        }
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

    static RegionKey regionKey(Object levelId, long packedChunk) {
        var chunkX = chunkX(packedChunk);
        var chunkZ = chunkZ(packedChunk);
        return new RegionKey(levelId, Math.floorDiv(chunkX, REGION_SIZE_CHUNKS), Math.floorDiv(chunkZ, REGION_SIZE_CHUNKS));
    }

    private static int chunkX(long packedChunk) {
        return (int) packedChunk;
    }

    private static int chunkZ(long packedChunk) {
        return (int) (packedChunk >> 32);
    }

    record RegionKey(Object levelId, int regionX, int regionZ) {

        RegionKey {
            Objects.requireNonNull(levelId, "levelId");
        }
    }

    private record WorkerSet(ExecutorService[] workers) {

        private static WorkerSet create(int configuredThreads) {
            var workerCount = workerThreadCount(configuredThreads);
            var workers = new ExecutorService[workerCount];
            var threadFactory = threadFactory();
            for (int i = 0; i < workers.length; i++) {
                workers[i] = Executors.newSingleThreadExecutor(threadFactory);
            }
            return new WorkerSet(workers);
        }

        private <T> CompletableFuture<T> submit(RegionKey key, Supplier<T> task) {
            return CompletableFuture.supplyAsync(task, worker(key));
        }

        private ExecutorService worker(RegionKey key) {
            return workers[Math.floorMod(key.hashCode(), workers.length)];
        }

        private void shutdown() {
            for (var worker : workers) {
                worker.shutdown();
            }
        }

        private void shutdownNow() {
            for (var worker : workers) {
                worker.shutdownNow();
            }
        }

        private static ThreadFactory threadFactory() {
            var threadId = new AtomicLong();
            return task -> {
                var thread = new Thread(task, "Fand Region Worker-" + threadId.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            };
        }
    }
}
