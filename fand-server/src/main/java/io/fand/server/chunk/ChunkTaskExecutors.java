package io.fand.server.chunk;

import io.fand.server.config.FandConfig;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.TracingExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChunkTaskExecutors implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkTaskExecutors.class);
    private static final int MAX_AUTO_LOAD_THREADS = 8;
    private static final int MAX_AUTO_WORLDGEN_THREADS = 32;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile TracingExecutor loadExecutor;
    private volatile TracingExecutor worldgenExecutor;

    public ChunkTaskExecutors(FandConfig.Chunks config) {
        Objects.requireNonNull(config, "config");
        this.loadExecutor = create("Chunk Load", config.backgroundThreads);
        this.worldgenExecutor = create("Chunk Worldgen", config.worldgenThreads);
    }

    public TracingExecutor loadExecutor() {
        return this.loadExecutor;
    }

    public TracingExecutor worldgenExecutor() {
        return this.worldgenExecutor;
    }

    public void reconfigure(FandConfig.Chunks config) {
        Objects.requireNonNull(config, "config");
        if (this.closed.get()) {
            throw new IllegalStateException("chunk task executors are closed");
        }

        TracingExecutor nextLoad = create("Chunk Load", config.backgroundThreads);
        TracingExecutor nextWorldgen = create("Chunk Worldgen", config.worldgenThreads);
        TracingExecutor previousLoad = this.loadExecutor;
        TracingExecutor previousWorldgen = this.worldgenExecutor;
        this.loadExecutor = nextLoad;
        this.worldgenExecutor = nextWorldgen;
        shutdown(previousLoad);
        shutdown(previousWorldgen);
    }

    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }

        shutdownAndAwait(this.loadExecutor);
        shutdownAndAwait(this.worldgenExecutor);
    }

    static int threadCount(final int configuredThreads) {
        if (configuredThreads < 0) {
            throw new IllegalArgumentException("configuredThreads must not be negative");
        }
        if (configuredThreads > 0) {
            return configuredThreads;
        }
        return Math.max(1, Math.min(MAX_AUTO_LOAD_THREADS, Runtime.getRuntime().availableProcessors() / 4));
    }

    static int worldgenThreadCount(final int configuredThreads) {
        if (configuredThreads < 0) {
            throw new IllegalArgumentException("configuredThreads must not be negative");
        }
        if (configuredThreads > 0) {
            return configuredThreads;
        }
        return Math.max(2, Math.min(MAX_AUTO_WORLDGEN_THREADS, Runtime.getRuntime().availableProcessors()));
    }

    private static TracingExecutor create(final String name, final int configuredThreads) {
        int threadCount = "Chunk Worldgen".equals(name) ? worldgenThreadCount(configuredThreads) : threadCount(configuredThreads);
        ExecutorService service = Executors.newFixedThreadPool(threadCount, threadFactory(name));
        return new TracingExecutor(service);
    }

    private static java.util.concurrent.ThreadFactory threadFactory(final String name) {
        AtomicInteger sequence = new AtomicInteger(1);
        return task -> {
            Thread thread = new Thread(task, "Fand " + name + " #" + sequence.getAndIncrement());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.setUncaughtExceptionHandler((runningThread, throwable) ->
                    LOGGER.error("Uncaught exception in thread {}", runningThread.getName(), throwable));
            return thread;
        };
    }

    private static void shutdown(final TracingExecutor executor) {
        executor.service().shutdown();
    }

    private static void shutdownAndAwait(final TracingExecutor executor) {
        executor.shutdownAndAwait(3L, TimeUnit.SECONDS);
    }
}
