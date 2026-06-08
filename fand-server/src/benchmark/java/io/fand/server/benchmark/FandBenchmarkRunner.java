package io.fand.server.benchmark;

import io.fand.api.event.Event;
import io.fand.server.event.EventDispatcher;
import io.fand.server.performance.ServerPerformanceTracker;
import io.fand.server.scheduler.TaskScheduler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class FandBenchmarkRunner {

    private static final int WARMUP_ROUNDS = Integer.getInteger("fand.benchmark.warmup", 3);
    private static final int MEASUREMENT_ROUNDS = Integer.getInteger("fand.benchmark.measurements", 5);
    private static final int SCALE = Math.max(1, Integer.getInteger("fand.benchmark.scale", 1));

    private FandBenchmarkRunner() {
    }

    public static void main(String[] args) {
        var benchmarks = List.of(
                benchmark("events.fire.noListeners", 250_000, FandBenchmarkRunner::eventNoListeners),
                benchmark("events.fire.oneListener", 200_000, FandBenchmarkRunner::eventOneListener),
                benchmark("events.fire.tenListeners", 100_000, FandBenchmarkRunner::eventTenListeners),
                benchmark("events.hasListeners.cached", 500_000, FandBenchmarkRunner::hasListenersCached),
                benchmark("performance.snapshot.cached", 500_000, FandBenchmarkRunner::performanceSnapshotCached),
                benchmark("performance.recordTick", 200_000, FandBenchmarkRunner::performanceRecordTick),
                benchmark("performance.recordAndSnapshot", 500, FandBenchmarkRunner::performanceRecordAndSnapshot),
                benchmark("scheduler.tick.empty", 500_000, FandBenchmarkRunner::schedulerTickEmpty),
                benchmark("scheduler.tick.readyTasks", 20_000, FandBenchmarkRunner::schedulerTickReadyTasks)
        );

        System.out.println("Fand benchmark runner");
        System.out.println("warmupRounds=" + WARMUP_ROUNDS
                + ", measurementRounds=" + MEASUREMENT_ROUNDS
                + ", scale=" + SCALE);
        System.out.println();
        System.out.printf("%-34s %14s %14s %9s%n", "benchmark", "ops/s", "ns/op", "rounds");
        System.out.printf("%-34s %14s %14s %9s%n", "-".repeat(34), "-".repeat(14), "-".repeat(14), "-".repeat(9));

        var results = new ArrayList<BenchmarkResult>();
        for (var benchmark : benchmarks) {
            var result = benchmark.run();
            results.add(result);
            System.out.printf(
                    Locale.ROOT,
                    "%-34s %,14.0f %,14.1f %9d%n",
                    result.name(),
                    result.operationsPerSecond(),
                    result.nanosPerOperation(),
                    result.rounds());
        }

        var slowest = results.stream()
                .max(Comparator.comparingDouble(BenchmarkResult::nanosPerOperation))
                .orElseThrow();
        System.out.println();
        System.out.println("slowest=" + slowest.name() + " (" + format(slowest.nanosPerOperation()) + " ns/op)");
    }

    private static Benchmark benchmark(String name, int iterations, BenchFactory factory) {
        return new Benchmark(name, iterations * SCALE, factory);
    }

    private static BenchTask eventNoListeners() {
        var bus = new EventDispatcher();
        var event = new BenchmarkEvent();
        return task(() -> bus.fire(event));
    }

    private static BenchTask eventOneListener() {
        var bus = new EventDispatcher();
        var counter = new AtomicInteger();
        bus.subscribe(BenchmarkEvent.class, event -> counter.incrementAndGet());
        var event = new BenchmarkEvent();
        return task(() -> bus.fire(event));
    }

    private static BenchTask eventTenListeners() {
        var bus = new EventDispatcher();
        var counter = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            bus.subscribe(BenchmarkEvent.class, event -> counter.incrementAndGet());
        }
        var event = new BenchmarkEvent();
        return task(() -> bus.fire(event));
    }

    private static BenchTask hasListenersCached() {
        var bus = new EventDispatcher();
        bus.subscribe(BenchmarkEvent.class, event -> {});
        bus.hasListeners(BenchmarkEvent.class);
        return task(() -> {
            if (!bus.hasListeners(BenchmarkEvent.class)) {
                throw new AssertionError("expected listeners");
            }
        });
    }

    private static BenchTask performanceSnapshotCached() {
        var tracker = new ServerPerformanceTracker();
        tracker.recordTick(0L, 10_000_000L);
        tracker.snapshot();
        return task(tracker::snapshot, tracker::close);
    }

    private static BenchTask performanceRecordAndSnapshot() {
        var tracker = new ServerPerformanceTracker();
        var tick = new AtomicLong();
        return task(() -> {
            var index = tick.getAndIncrement();
            tracker.recordTick(index * 50_000_000L, 10_000_000L + (index % 5) * 1_000_000L);
            tracker.snapshot();
        }, tracker::close);
    }

    private static BenchTask performanceRecordTick() {
        var tracker = new ServerPerformanceTracker();
        var tick = new AtomicLong();
        return task(() -> {
            var index = tick.getAndIncrement();
            tracker.recordTick(index * 50_000_000L, 10_000_000L + (index % 5) * 1_000_000L);
        }, tracker::close);
    }

    private static BenchTask schedulerTickEmpty() {
        var scheduler = new TaskScheduler(1);
        return task(scheduler::tick, scheduler::close);
    }

    private static BenchTask schedulerTickReadyTasks() {
        var scheduler = new TaskScheduler(1);
        return task(() -> {
            scheduler.runMain(() -> {});
            scheduler.runMainAfterTicks(() -> {}, 0L);
            int executed = scheduler.tick();
            if (executed != 2) {
                throw new AssertionError("expected 2 executed tasks, got " + executed);
            }
        }, scheduler::close);
    }

    private static BenchTask task(Runnable run) {
        return task(run, () -> {});
    }

    private static BenchTask task(Runnable run, AutoCloseable close) {
        return new BenchTask(run, close);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    private record BenchmarkEvent() implements Event {
    }

    @FunctionalInterface
    private interface BenchFactory {
        BenchTask create();
    }

    private record Benchmark(String name, int iterations, BenchFactory factory) {

        BenchmarkResult run() {
            for (int i = 0; i < WARMUP_ROUNDS; i++) {
                measure(factory.create());
            }

            long totalNanos = 0L;
            for (int i = 0; i < MEASUREMENT_ROUNDS; i++) {
                totalNanos += measure(factory.create());
            }
            double operations = (double) iterations * MEASUREMENT_ROUNDS;
            double nanosPerOperation = totalNanos / operations;
            double operationsPerSecond = TimeUnit.SECONDS.toNanos(1) / nanosPerOperation;
            return new BenchmarkResult(name, operationsPerSecond, nanosPerOperation, MEASUREMENT_ROUNDS);
        }

        private long measure(BenchTask task) {
            try (task) {
                long start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    task.run();
                }
                return System.nanoTime() - start;
            } catch (Exception failure) {
                throw new RuntimeException("Benchmark failed: " + name, failure);
            }
        }
    }

    private static final class BenchTask implements AutoCloseable {

        private final Runnable run;
        private final AutoCloseable close;

        private BenchTask(Runnable run, AutoCloseable close) {
            this.run = run;
            this.close = close;
        }

        void run() {
            run.run();
        }

        @Override
        public void close() throws Exception {
            close.close();
        }
    }

    private record BenchmarkResult(String name, double operationsPerSecond, double nanosPerOperation, int rounds) {
    }
}
