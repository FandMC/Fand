package io.fand.server.performance;

import io.fand.api.performance.MetricStatistics;
import io.fand.api.performance.ServerPerformance;
import io.fand.api.performance.TickWindow;
import io.fand.api.performance.TickWindowSnapshot;
import java.util.Arrays;

public final class ServerPerformanceTracker {

    private static final int CAPACITY = TickWindow.FIFTEEN_MINUTES.ticks();
    private static final long TARGET_TICK_NANOS = 50_000_000L;
    private static final long IDLE_TICK_INTERVAL_TOLERANCE_NANOS = 2_000_000L;
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final long[] tickDurationsNanos = new long[CAPACITY];
    private final long[] tickIntervalsNanos = new long[CAPACITY];
    private final long[] tickIntervalWorkNanos = new long[CAPACITY];
    private long previousTickStartNanos = -1L;
    private long previousTickWorkNanos = -1L;
    private long tickCount;
    private long snapshotTickCount = -1L;
    private ServerPerformance cachedSnapshot;

    public synchronized void recordTick(long tickStartNanos, long tickDurationNanos) {
        recordTick(tickStartNanos, tickDurationNanos, 0L);
    }

    public synchronized void recordTick(long tickStartNanos, long tickDurationNanos, long taskExecutionNanos) {
        if (tickStartNanos < 0L) {
            throw new IllegalArgumentException("tickStartNanos must be >= 0");
        }
        if (tickDurationNanos < 0L) {
            throw new IllegalArgumentException("tickDurationNanos must be >= 0");
        }
        if (taskExecutionNanos < 0L) {
            throw new IllegalArgumentException("taskExecutionNanos must be >= 0");
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
    }

    public synchronized ServerPerformance snapshot() {
        if (cachedSnapshot != null && snapshotTickCount == tickCount) {
            return cachedSnapshot;
        }
        var snapshot = new ServerPerformance(
                window(TickWindow.ONE_SECOND),
                window(TickWindow.FIVE_SECONDS),
                window(TickWindow.TEN_SECONDS),
                window(TickWindow.FIFTEEN_SECONDS),
                window(TickWindow.ONE_MINUTE),
                window(TickWindow.FIVE_MINUTES),
                window(TickWindow.FIFTEEN_MINUTES),
                tickCount
        );
        cachedSnapshot = snapshot;
        snapshotTickCount = tickCount;
        return snapshot;
    }

    private TickWindowSnapshot window(TickWindow window) {
        int samples = sampleCount(window.ticks());
        if (samples == 0) {
            return new TickWindowSnapshot(
                    window,
                    new MetricStatistics(20.0, 20.0, 20.0, 20.0),
                    new MetricStatistics(50.0, 50.0, 50.0, 50.0),
                    new MetricStatistics(0.0, 0.0, 0.0, 0.0),
                    0.0,
                    0);
        }

        long[] durations = latest(tickDurationsNanos, samples);
        long[] intervals = latest(tickIntervalsNanos, samples);
        long[] intervalWork = latest(tickIntervalWorkNanos, samples);
        var mspt = millisecondStatistics(durations);
        return new TickWindowSnapshot(
                window,
                tpsStatistics(intervals, intervalWork),
                millisecondStatistics(intervals),
                mspt,
                mspt.average() / 50.0,
                samples);
    }

    private MetricStatistics tpsStatistics(long[] intervals, long[] intervalWork) {
        var raw = new double[intervals.length];
        long total = 0L;
        for (int i = 0; i < intervals.length; i++) {
            long interval = normalizeTpsInterval(intervals[i], intervalWork[i]);
            raw[i] = normalizeTps(NANOS_PER_SECOND / interval);
            total += interval;
        }
        double average = normalizeTps(intervals.length * NANOS_PER_SECOND / total);
        return new MetricStatistics(average, min(raw), max(raw), median(raw));
    }

    private MetricStatistics millisecondStatistics(long[] values) {
        var raw = new double[values.length];
        long total = 0L;
        for (int i = 0; i < values.length; i++) {
            long value = Math.max(1L, values[i]);
            raw[i] = value / NANOS_PER_MILLISECOND;
            total += value;
        }
        double average = total / (double) values.length / NANOS_PER_MILLISECOND;
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

    private long[] latest(long[] source, int samples) {
        var result = new long[samples];
        for (int offset = 0; offset < samples; offset++) {
            long sampleIndex = tickCount - 1L - offset;
            int index = (int) Math.floorMod(sampleIndex, source.length);
            result[samples - 1 - offset] = source[index];
        }
        return result;
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
}
