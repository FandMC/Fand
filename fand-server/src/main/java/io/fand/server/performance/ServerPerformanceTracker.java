package io.fand.server.performance;

import io.fand.api.performance.MetricStatistics;
import io.fand.api.performance.ServerPerformance;
import io.fand.api.performance.TickWindow;
import io.fand.api.performance.TickWindowSnapshot;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public final class ServerPerformanceTracker implements AutoCloseable {

    private static final int CAPACITY = TickWindow.FIFTEEN_MINUTES.ticks();
    private static final long TARGET_TICK_NANOS = 50_000_000L;
    private static final long IDLE_TICK_INTERVAL_TOLERANCE_NANOS = 2_000_000L;
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;
    private static final ServerPerformance INITIAL_SNAPSHOT = createSnapshot(Samples.empty(), 0L);

    private final Executor executor;
    private final AutoCloseable closeExecutor;
    private final java.util.function.LongSupplier chunkQueueDepthSupplier;
    private final long[] tickDurationsNanos = new long[CAPACITY];
    private final long[] tickIntervalsNanos = new long[CAPACITY];
    private final long[] tickIntervalWorkNanos = new long[CAPACITY];
    private volatile ServerPerformance publishedSnapshot = INITIAL_SNAPSHOT;
    private long previousTickStartNanos = -1L;
    private long previousTickWorkNanos = -1L;
    private long tickCount;
    private boolean recomputeQueued;
    private boolean closed;

    public ServerPerformanceTracker() {
        this(() -> 0L);
    }

    public ServerPerformanceTracker(java.util.function.LongSupplier chunkQueueDepthSupplier) {
        var executorService = Executors.newSingleThreadExecutor(ServerPerformanceTracker::snapshotThread);
        this.executor = executorService;
        this.closeExecutor = executorService::shutdownNow;
        this.chunkQueueDepthSupplier = chunkQueueDepthSupplier;
    }

    ServerPerformanceTracker(Executor executor, java.util.function.LongSupplier chunkQueueDepthSupplier) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.closeExecutor = () -> {};
        this.chunkQueueDepthSupplier = chunkQueueDepthSupplier;
    }

    public void recordTick(long tickStartNanos, long tickDurationNanos) {
        recordTick(tickStartNanos, tickDurationNanos, 0L);
    }

    public void recordTick(long tickStartNanos, long tickDurationNanos, long taskExecutionNanos) {
        if (tickStartNanos < 0L) {
            throw new IllegalArgumentException("tickStartNanos must be >= 0");
        }
        if (tickDurationNanos < 0L) {
            throw new IllegalArgumentException("tickDurationNanos must be >= 0");
        }
        if (taskExecutionNanos < 0L) {
            throw new IllegalArgumentException("taskExecutionNanos must be >= 0");
        }

        boolean shouldSchedule;
        synchronized (this) {
            if (closed) {
                return;
            }

            long tickWorkNanos = tickDurationNanos + taskExecutionNanos;
            long tickIntervalNanos = previousTickStartNanos < 0L
                    ? Math.max(TARGET_TICK_NANOS, tickWorkNanos)
                    : tickStartNanos - previousTickStartNanos;
            previousTickStartNanos = tickStartNanos;
            if (tickIntervalNanos <= 0L) {
                tickIntervalNanos = TARGET_TICK_NANOS;
            }
            long intervalWorkNanos = previousTickWorkNanos < 0L ? tickWorkNanos : previousTickWorkNanos;
            previousTickWorkNanos = tickWorkNanos;

            int index = (int) (tickCount % tickDurationsNanos.length);
            tickDurationsNanos[index] = tickWorkNanos;
            tickIntervalsNanos[index] = tickIntervalNanos;
            tickIntervalWorkNanos[index] = intervalWorkNanos;
            tickCount++;
            shouldSchedule = markRecomputeNeeded();
        }

        if (shouldSchedule) {
            scheduleRecompute();
        }
    }

    public ServerPerformance snapshot() {
        return publishedSnapshot;
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            recomputeQueued = false;
        }
        try {
            closeExecutor.close();
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to close performance tracker", failure);
        }
    }

    private boolean markRecomputeNeeded() {
        if (closed || recomputeQueued) {
            return false;
        }
        recomputeQueued = true;
        return true;
    }

    private void scheduleRecompute() {
        try {
            executor.execute(this::recomputeSnapshots);
        } catch (RejectedExecutionException failure) {
            boolean shouldThrow;
            synchronized (this) {
                recomputeQueued = false;
                shouldThrow = !closed;
            }
            if (shouldThrow) {
                throw failure;
            }
        }
    }

    private void recomputeSnapshots() {
        while (true) {
            Samples samples;
            synchronized (this) {
                if (closed) {
                    recomputeQueued = false;
                    return;
                }
                samples = captureSamples();
            }

            ServerPerformance snapshot = createSnapshot(samples, chunkQueueDepthSupplier.getAsLong());

            synchronized (this) {
                if (!closed && snapshot.tickCount() >= publishedSnapshot.tickCount()) {
                    publishedSnapshot = snapshot;
                }
                if (closed || samples.tickCount() == tickCount) {
                    recomputeQueued = false;
                    return;
                }
            }
        }
    }

    private Samples captureSamples() {
        int samples = sampleCount(CAPACITY);
        var durations = new long[samples];
        var intervals = new long[samples];
        var intervalWork = new long[samples];
        for (int offset = 0; offset < samples; offset++) {
            long sampleIndex = tickCount - samples + offset;
            int index = (int) Math.floorMod(sampleIndex, tickDurationsNanos.length);
            durations[offset] = tickDurationsNanos[index];
            intervals[offset] = tickIntervalsNanos[index];
            intervalWork[offset] = tickIntervalWorkNanos[index];
        }
        return new Samples(tickCount, durations, intervals, intervalWork);
    }

    private static ServerPerformance createSnapshot(Samples samples, long chunkQueueDepth) {
        return new ServerPerformance(
                window(TickWindow.ONE_SECOND, samples),
                window(TickWindow.FIVE_SECONDS, samples),
                window(TickWindow.TEN_SECONDS, samples),
                window(TickWindow.FIFTEEN_SECONDS, samples),
                window(TickWindow.ONE_MINUTE, samples),
                window(TickWindow.FIVE_MINUTES, samples),
                window(TickWindow.FIFTEEN_MINUTES, samples),
                samples.tickCount(),
                chunkQueueDepth
        );
    }

    private static TickWindowSnapshot window(TickWindow window, Samples source) {
        int samples = Math.min(source.sampleCount(), window.ticks());
        if (samples == 0) {
            return new TickWindowSnapshot(
                    window,
                    new MetricStatistics(20.0, 20.0, 20.0, 20.0),
                    new MetricStatistics(50.0, 50.0, 50.0, 50.0),
                    new MetricStatistics(0.0, 0.0, 0.0, 0.0),
                    0.0,
                    0);
        }

        int start = source.sampleCount() - samples;
        var mspt = millisecondStatistics(source.durations(), start, samples);
        return new TickWindowSnapshot(
                window,
                tpsStatistics(source.intervals(), source.intervalWork(), start, samples),
                millisecondStatistics(source.intervals(), start, samples),
                mspt,
                mspt.average() / 50.0,
                samples);
    }

    private static MetricStatistics tpsStatistics(long[] intervals, long[] intervalWork, int start, int length) {
        var raw = new double[length];
        long total = 0L;
        for (int i = 0; i < length; i++) {
            int index = start + i;
            long interval = normalizeTpsInterval(intervals[index], intervalWork[index]);
            raw[i] = normalizeTps(NANOS_PER_SECOND / interval);
            total += interval;
        }
        double average = normalizeTps(length * NANOS_PER_SECOND / total);
        return new MetricStatistics(average, min(raw), max(raw), median(raw));
    }

    private static MetricStatistics millisecondStatistics(long[] values, int start, int length) {
        var raw = new double[length];
        long total = 0L;
        for (int i = 0; i < length; i++) {
            long value = Math.max(1L, values[start + i]);
            raw[i] = value / NANOS_PER_MILLISECOND;
            total += value;
        }
        double average = total / (double) length / NANOS_PER_MILLISECOND;
        return new MetricStatistics(average, min(raw), max(raw), median(raw));
    }

    private static long normalizeTpsInterval(long intervalNanos, long tickWorkNanos) {
        // Vanilla sleep can wake a healthy idle server slightly late. Keep the
        // raw interval exposed, but avoid reporting that tiny scheduling noise as lag.
        if (tickWorkNanos <= TARGET_TICK_NANOS
                && intervalNanos > TARGET_TICK_NANOS
                && intervalNanos <= TARGET_TICK_NANOS + IDLE_TICK_INTERVAL_TOLERANCE_NANOS) {
            return TARGET_TICK_NANOS;
        }
        return Math.max(1L, intervalNanos);
    }

    private static double normalizeTps(double value) {
        return Math.min(20.0, value);
    }

    private int sampleCount(int requested) {
        return (int) Math.min(Math.min(tickCount, tickDurationsNanos.length), requested);
    }

    private static double min(double[] values) {
        double min = Double.POSITIVE_INFINITY;
        for (double value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private static double max(double[] values) {
        double max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private static double median(double[] values) {
        var copy = values.clone();
        Arrays.sort(copy);
        int middle = copy.length / 2;
        if ((copy.length & 1) == 0) {
            return (copy[middle - 1] + copy[middle]) / 2.0;
        }
        return copy[middle];
    }

    private static Thread snapshotThread(Runnable task) {
        var thread = new Thread(task, "Fand Performance Snapshot");
        thread.setDaemon(true);
        return thread;
    }

    private record Samples(long tickCount, long[] durations, long[] intervals, long[] intervalWork) {

        private static Samples empty() {
            return new Samples(0L, new long[0], new long[0], new long[0]);
        }

        private int sampleCount() {
            return durations.length;
        }
    }
}
