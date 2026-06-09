package io.fand.server.benchmark;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.server.event.EventDispatcher;
import io.fand.server.performance.ServerPerformanceTracker;
import io.fand.server.scheduler.TaskScheduler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
                benchmark("events.fire.cancelled", 150_000, FandBenchmarkRunner::eventCancelled),
                benchmark("events.hasListeners.cached", 500_000, FandBenchmarkRunner::hasListenersCached),
                benchmark("performance.snapshot.cached", 500_000, FandBenchmarkRunner::performanceSnapshotCached),
                benchmark("performance.recordTick", 200_000, FandBenchmarkRunner::performanceRecordTick),
                benchmark("performance.recordAndSnapshot", 500, FandBenchmarkRunner::performanceRecordAndSnapshot),
                benchmark("scheduler.tick.empty", 500_000, FandBenchmarkRunner::schedulerTickEmpty),
                benchmark("scheduler.tick.readyTasks", 20_000, FandBenchmarkRunner::schedulerTickReadyTasks),
                benchmark("wrapper.cache.hit", 500_000, FandBenchmarkRunner::wrapperCacheHit),
                benchmark("world.entitiesInBox.scan256", 20_000, FandBenchmarkRunner::entitiesInBoxScan),
                benchmark("world.rayTraceEntity.scan256", 10_000, FandBenchmarkRunner::rayTraceEntityScan)
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

    private static BenchTask eventCancelled() {
        var bus = new EventDispatcher();
        bus.subscribe(CancellableBenchmarkEvent.class, event -> event.setCancelled(true));
        return task(() -> {
            var event = new CancellableBenchmarkEvent();
            bus.fire(event);
            if (!event.cancelled()) {
                throw new AssertionError("expected cancelled event");
            }
        });
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

    private static BenchTask wrapperCacheHit() {
        var cache = Caffeine.newBuilder()
                .expireAfterAccess(java.time.Duration.ofMinutes(5))
                .maximumSize(8192)
                .build();
        var id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var wrapper = new Object();
        cache.put(id, wrapper);
        return task(() -> {
            if (cache.getIfPresent(id) != wrapper) {
                throw new AssertionError("expected cache hit");
            }
        });
    }

    private static BenchTask entitiesInBoxScan() {
        var entities = syntheticEntities();
        var box = new AABB(12.0, 60.0, 12.0, 34.0, 76.0, 34.0);
        return task(() -> {
            int count = 0;
            for (var entity : entities) {
                if (entity.intersects(box)) {
                    count++;
                }
            }
            if (count != 36) {
                throw new AssertionError("expected 36 intersections, got " + count);
            }
        });
    }

    private static BenchTask rayTraceEntityScan() {
        var entities = syntheticEntities();
        var from = new Vec3(0.0, 65.0, 0.0);
        var to = new Vec3(64.0, 65.0, 64.0);
        return task(() -> {
            double nearestDistanceSquared = Double.MAX_VALUE;
            BenchEntity nearest = null;
            for (var entity : entities) {
                var hit = entity.box().inflate(0.1).clip(from, to);
                if (hit.isPresent()) {
                    double distanceSquared = from.distanceToSqr(hit.get());
                    if (distanceSquared < nearestDistanceSquared) {
                        nearestDistanceSquared = distanceSquared;
                        nearest = entity;
                    }
                }
            }
            if (nearest == null || nearest.index() != 0) {
                throw new AssertionError("expected first entity hit");
            }
        });
    }

    private static List<BenchEntity> syntheticEntities() {
        var entities = new ArrayList<BenchEntity>(256);
        for (int i = 0; i < 256; i++) {
            double x = (i % 16) * 4.0 + 0.5;
            double y = 64.0 + (i % 5);
            double z = (i / 16) * 4.0 + 0.5;
            entities.add(new BenchEntity(i, new AABB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3)));
        }
        return List.copyOf(entities);
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

    private static final class CancellableBenchmarkEvent implements Event, Cancellable {
        private boolean cancelled;

        @Override
        public boolean cancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    private record BenchEntity(int index, AABB box) {
        boolean intersects(AABB other) {
            return box.intersects(other);
        }
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
