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
    private static final int NAME_WIDTH = 44;
    private static final double ENTITY_SPACING = 4.0;
    private static final double RAY_SEGMENT_BLOCKS = 16.0;

    private FandBenchmarkRunner() {
    }

    public static void main(String[] args) {
        var benchmarks = List.of(
                benchmark("events.fire.noListeners", 250_000, FandBenchmarkRunner::eventNoListeners),
                benchmark("events.fire.oneListener", 200_000, FandBenchmarkRunner::eventOneListener),
                benchmark("events.fire.tenListeners", 100_000, FandBenchmarkRunner::eventTenListeners),
                benchmark("events.fire.cancelled", 150_000, FandBenchmarkRunner::eventCancelled),
                benchmark("events.fire.100Listeners", 25_000, FandBenchmarkRunner::eventHundredListeners),
                benchmark("events.fire.cancelled.100Listeners", 25_000, FandBenchmarkRunner::eventCancelledHundredListeners),
                benchmark("events.hasListeners.cached", 500_000, FandBenchmarkRunner::hasListenersCached),
                benchmark("performance.snapshot.cached", 500_000, FandBenchmarkRunner::performanceSnapshotCached),
                benchmark("performance.recordTick", 200_000, FandBenchmarkRunner::performanceRecordTick),
                benchmark("performance.recordAndSnapshot", 500, FandBenchmarkRunner::performanceRecordAndSnapshot),
                benchmark("scheduler.tick.empty", 500_000, FandBenchmarkRunner::schedulerTickEmpty),
                benchmark("scheduler.tick.readyTasks", 20_000, FandBenchmarkRunner::schedulerTickReadyTasks),
                benchmark("wrapper.cache.hit", 500_000, FandBenchmarkRunner::wrapperCacheHit),
                benchmark("world.entitiesInBox.indexed256", 50_000, FandBenchmarkRunner::entitiesInBoxIndexed),
                benchmark("world.entitiesInBox.indexed4096", 50_000, FandBenchmarkRunner::entitiesInBoxIndexedLarge),
                benchmark("world.entitiesInBox.wrapHits4096", 20_000, FandBenchmarkRunner::entitiesInBoxWrapHitsLarge),
                benchmark("world.countEntitiesInBox.indexed4096", 50_000, FandBenchmarkRunner::countEntitiesInBoxIndexedLarge),
                benchmark("world.firstEntityInBox.indexed4096", 500_000, FandBenchmarkRunner::firstEntityInBoxIndexedLarge),
                benchmark("world.forEachEntityInBox.wrapHits4096", 20_000, FandBenchmarkRunner::forEachEntityInBoxWrapHitsLarge),
                benchmark("world.rayTraceEntity.segmented256", 25_000, FandBenchmarkRunner::rayTraceEntitySegmented),
                benchmark("world.rayTraceEntity.segmented4096", 20_000, FandBenchmarkRunner::rayTraceEntitySegmentedLarge),
                benchmark("world.rayTraceEntity.snapshotSegmented4096", 20_000, FandBenchmarkRunner::rayTraceEntitySnapshotSegmentedLarge),
                benchmark("world.rayTraceEntity.computeSegmented4096", 20_000, FandBenchmarkRunner::rayTraceEntityComputeSegmentedLarge),
                benchmark("rollback.blockPlace.synthetic", 20_000, FandBenchmarkRunner::blockPlaceRollbackSynthetic)
        );

        System.out.println("Fand benchmark runner");
        System.out.println("warmupRounds=" + WARMUP_ROUNDS
                + ", measurementRounds=" + MEASUREMENT_ROUNDS
                + ", scale=" + SCALE);
        System.out.println();
        System.out.printf("%-" + NAME_WIDTH + "s %14s %14s %9s%n", "benchmark", "ops/s", "ns/op", "rounds");
        System.out.printf("%-" + NAME_WIDTH + "s %14s %14s %9s%n",
                "-".repeat(NAME_WIDTH),
                "-".repeat(14),
                "-".repeat(14),
                "-".repeat(9));

        var results = new ArrayList<BenchmarkResult>();
        for (var benchmark : benchmarks) {
            var result = benchmark.run();
            results.add(result);
            System.out.printf(
                    Locale.ROOT,
                    "%-" + NAME_WIDTH + "s %,14.0f %,14.1f %9d%n",
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

    private static BenchTask eventHundredListeners() {
        var bus = new EventDispatcher();
        var counter = new AtomicInteger();
        for (int i = 0; i < 100; i++) {
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

    private static BenchTask eventCancelledHundredListeners() {
        var bus = new EventDispatcher();
        var counter = new AtomicInteger();
        for (int i = 0; i < 99; i++) {
            bus.subscribe(CancellableBenchmarkEvent.class, event -> counter.incrementAndGet());
        }
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

    private static BenchTask entitiesInBoxIndexed() {
        var entities = syntheticEntityIndex(256);
        var box = new AABB(12.0, 60.0, 12.0, 34.0, 76.0, 34.0);
        var matches = new ArrayList<BenchEntity>(64);
        return task(() -> {
            matches.clear();
            entities.collect(box, matches);
            if (matches.size() != 36) {
                throw new AssertionError("expected 36 intersections, got " + matches.size());
            }
        });
    }

    private static BenchTask entitiesInBoxIndexedLarge() {
        var entities = syntheticEntityIndex(4096);
        var box = new AABB(32.0, 60.0, 32.0, 96.0, 76.0, 96.0);
        var matches = new ArrayList<BenchEntity>(256);
        return task(() -> {
            matches.clear();
            entities.collect(box, matches);
            if (matches.size() != 256) {
                throw new AssertionError("expected 256 intersections, got " + matches.size());
            }
        });
    }

    private static BenchTask entitiesInBoxWrapHitsLarge() {
        var entities = syntheticEntityIndex(4096);
        var wrappers = new Object[4096];
        for (int i = 0; i < wrappers.length; i++) {
            wrappers[i] = new Object();
        }
        var box = new AABB(32.0, 60.0, 32.0, 96.0, 76.0, 96.0);
        var matches = new ArrayList<BenchEntity>(256);
        var wrapped = new ArrayList<Object>(256);
        return task(() -> {
            matches.clear();
            wrapped.clear();
            entities.collect(box, matches);
            for (var entity : matches) {
                wrapped.add(wrappers[entity.index()]);
            }
            if (wrapped.size() != 256) {
                throw new AssertionError("expected 256 wrapped entities, got " + wrapped.size());
            }
        });
    }

    private static BenchTask countEntitiesInBoxIndexedLarge() {
        var entities = syntheticEntityIndex(4096);
        var box = new AABB(32.0, 60.0, 32.0, 96.0, 76.0, 96.0);
        return task(() -> {
            int count = entities.count(box);
            if (count != 256) {
                throw new AssertionError("expected 256 intersections, got " + count);
            }
        });
    }

    private static BenchTask firstEntityInBoxIndexedLarge() {
        var entities = syntheticEntityIndex(4096);
        var box = new AABB(32.0, 60.0, 32.0, 96.0, 76.0, 96.0);
        return task(() -> {
            var entity = entities.first(box);
            if (entity == null || entity.index() != 520) {
                throw new AssertionError("expected first indexed entity 520, got " + entity);
            }
        });
    }

    private static BenchTask forEachEntityInBoxWrapHitsLarge() {
        var entities = syntheticEntityIndex(4096);
        var wrappers = new Object[4096];
        for (int i = 0; i < wrappers.length; i++) {
            wrappers[i] = new Object();
        }
        var box = new AABB(32.0, 60.0, 32.0, 96.0, 76.0, 96.0);
        var counter = new AtomicInteger();
        return task(() -> {
            counter.set(0);
            entities.forEach(box, entity -> {
                if (wrappers[entity.index()] != null) {
                    counter.incrementAndGet();
                }
            });
            if (counter.get() != 256) {
                throw new AssertionError("expected 256 visited entities, got " + counter.get());
            }
        });
    }

    private static BenchTask rayTraceEntitySegmented() {
        var entities = syntheticEntityIndex(256);
        var from = new Vec3(0.0, 65.0, 0.0);
        var to = new Vec3(64.0, 65.0, 64.0);
        double distance = Math.sqrt(from.distanceToSqr(to));
        return task(() -> {
            var nearest = rayTraceSegmented(entities, from, to, distance);
            if (nearest == null || nearest.index() != 0) {
                throw new AssertionError("expected first entity hit");
            }
        });
    }

    private static BenchTask rayTraceEntitySegmentedLarge() {
        var entities = syntheticEntityIndex(4096);
        var from = new Vec3(0.0, 65.0, 0.0);
        var to = new Vec3(256.0, 65.0, 256.0);
        double distance = Math.sqrt(from.distanceToSqr(to));
        return task(() -> {
            var nearest = rayTraceSegmented(entities, from, to, distance);
            if (nearest == null || nearest.index() != 0) {
                throw new AssertionError("expected first entity hit");
            }
        });
    }

    private static BenchTask rayTraceEntitySnapshotSegmentedLarge() {
        var entities = syntheticEntityIndex(4096);
        var from = new Vec3(0.0, 65.0, 0.0);
        var to = new Vec3(256.0, 65.0, 256.0);
        double distance = Math.sqrt(from.distanceToSqr(to));
        return task(() -> {
            var snapshots = snapshotSegmented(entities, from, to, distance);
            if (snapshots.size <= 0 || snapshots.size >= 4096) {
                throw new AssertionError("expected segmented snapshots, got " + snapshots.size);
            }
        });
    }

    private static BenchTask rayTraceEntityComputeSegmentedLarge() {
        var index = syntheticEntityIndex(4096);
        var from = new Vec3(0.0, 65.0, 0.0);
        var to = new Vec3(256.0, 65.0, 256.0);
        double distance = Math.sqrt(from.distanceToSqr(to));
        var snapshots = snapshotSegmented(index, from, to, distance);
        return task(() -> {
            var nearest = traceSnapshots(snapshots, from, to, Double.MAX_VALUE);
            if (nearest == null || nearest.index() != 0) {
                throw new AssertionError("expected first entity hit");
            }
        });
    }

    private static BenchTask blockPlaceRollbackSynthetic() {
        var before = syntheticBlockStates();
        var current = syntheticBlockStates();
        return task(() -> {
            var restored = new java.util.HashMap<BenchBlockPos, Integer>(current);
            for (var entry : before.entrySet()) {
                restored.put(entry.getKey(), entry.getValue());
            }
            if (!restored.equals(before)) {
                throw new AssertionError("expected rollback snapshot to match original states");
            }
        });
    }

    private static List<BenchEntity> syntheticEntities(int count) {
        int side = (int) Math.ceil(Math.sqrt(count));
        var entities = new ArrayList<BenchEntity>(count);
        for (int i = 0; i < count; i++) {
            double x = (i % side) * 4.0 + 0.5;
            double y = 64.0 + (i % 5);
            double z = (i / side) * 4.0 + 0.5;
            entities.add(new BenchEntity(i, new AABB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3)));
        }
        return List.copyOf(entities);
    }

    private static BenchEntityIndex syntheticEntityIndex(int count) {
        int side = (int) Math.ceil(Math.sqrt(count));
        var cells = new BenchEntity[side * side];
        for (int i = 0; i < count; i++) {
            double x = (i % side) * ENTITY_SPACING + 0.5;
            double y = 64.0 + (i % 5);
            double z = (i / side) * ENTITY_SPACING + 0.5;
            cells[i] = new BenchEntity(i, new AABB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3));
        }
        return new BenchEntityIndex(cells, side, count);
    }

    private static BenchRayHit rayTraceSegmented(BenchEntityIndex entities, Vec3 from, Vec3 to, double distance) {
        var direction = to.subtract(from).normalize();
        double nearestDistanceSquared = Double.MAX_VALUE;
        BenchRayHit nearest = null;
        var candidates = new ArrayList<BenchEntity>(32);
        for (double segmentStart = 0.0; segmentStart < distance; segmentStart += RAY_SEGMENT_BLOCKS) {
            double segmentEnd = Math.min(distance, segmentStart + RAY_SEGMENT_BLOCKS);
            var segmentFrom = from.add(direction.scale(segmentStart));
            var segmentTo = from.add(direction.scale(segmentEnd));
            var segmentBox = new AABB(segmentFrom, segmentTo).inflate(1.0);
            candidates.clear();
            entities.collect(segmentBox, candidates);
            for (var entity : candidates) {
                var hit = traceBox(
                        entity.index(),
                        entity.box().minX - 0.1,
                        entity.box().minY - 0.1,
                        entity.box().minZ - 0.1,
                        entity.box().maxX + 0.1,
                        entity.box().maxY + 0.1,
                        entity.box().maxZ + 0.1,
                        from,
                        to,
                        nearestDistanceSquared);
                if (hit != null && hit.distanceSquared() <= nearestDistanceSquared) {
                    nearestDistanceSquared = hit.distanceSquared();
                    nearest = hit;
                }
            }
            if (nearest != null && nearestDistanceSquared <= segmentEnd * segmentEnd) {
                break;
            }
        }
        return nearest;
    }

    private static BenchRaySnapshot snapshotSegmented(BenchEntityIndex entities, Vec3 from, Vec3 to, double distance) {
        var direction = to.subtract(from).normalize();
        var snapshots = new BenchRaySnapshot(64);
        var candidates = new ArrayList<BenchEntity>(32);
        for (double segmentStart = 0.0; segmentStart < distance; segmentStart += RAY_SEGMENT_BLOCKS) {
            double segmentEnd = Math.min(distance, segmentStart + RAY_SEGMENT_BLOCKS);
            var segmentFrom = from.add(direction.scale(segmentStart));
            var segmentTo = from.add(direction.scale(segmentEnd));
            var segmentBox = new AABB(segmentFrom, segmentTo).inflate(1.0);
            candidates.clear();
            entities.collect(segmentBox, candidates);
            for (var entity : candidates) {
                snapshots.add(entity.index(), entity.box(), 0.1);
            }
        }
        return snapshots;
    }

    private static java.util.Map<BenchBlockPos, Integer> syntheticBlockStates() {
        var states = new java.util.HashMap<BenchBlockPos, Integer>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    states.put(new BenchBlockPos(x, y, z), (x + 2) * 31 + (y + 2) * 17 + (z + 2));
                }
            }
        }
        return java.util.Map.copyOf(states);
    }

    private static BenchRayHit traceSnapshots(BenchRaySnapshot entities, Vec3 from, Vec3 to, double maxDistanceSquared) {
        BenchRayHit nearest = null;
        double nearestDistanceSquared = maxDistanceSquared;
        for (int i = 0; i < entities.size; i++) {
            var hit = traceBox(
                    entities.indices[i],
                    entities.minX[i],
                    entities.minY[i],
                    entities.minZ[i],
                    entities.maxX[i],
                    entities.maxY[i],
                    entities.maxZ[i],
                    from,
                    to,
                    nearestDistanceSquared);
            if (hit != null && hit.distanceSquared() <= nearestDistanceSquared) {
                nearestDistanceSquared = hit.distanceSquared();
                nearest = hit;
            }
        }
        return nearest;
    }

    private static BenchRayHit traceBox(
            int index,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            Vec3 from,
            Vec3 to,
            double maxDistanceSquared
    ) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double tMin = 0.0;
        double tMax = 1.0;

        if (Math.abs(dx) < 1.0E-12) {
            if (from.x < minX || from.x > maxX) {
                return null;
            }
        } else {
            double inv = 1.0 / dx;
            double near = (minX - from.x) * inv;
            double far = (maxX - from.x) * inv;
            if (near > far) {
                double swap = near;
                near = far;
                far = swap;
            }
            tMin = Math.max(tMin, near);
            tMax = Math.min(tMax, far);
            if (tMin > tMax) {
                return null;
            }
        }

        if (Math.abs(dy) < 1.0E-12) {
            if (from.y < minY || from.y > maxY) {
                return null;
            }
        } else {
            double inv = 1.0 / dy;
            double near = (minY - from.y) * inv;
            double far = (maxY - from.y) * inv;
            if (near > far) {
                double swap = near;
                near = far;
                far = swap;
            }
            tMin = Math.max(tMin, near);
            tMax = Math.min(tMax, far);
            if (tMin > tMax) {
                return null;
            }
        }

        if (Math.abs(dz) < 1.0E-12) {
            if (from.z < minZ || from.z > maxZ) {
                return null;
            }
        } else {
            double inv = 1.0 / dz;
            double near = (minZ - from.z) * inv;
            double far = (maxZ - from.z) * inv;
            if (near > far) {
                double swap = near;
                near = far;
                far = swap;
            }
            tMin = Math.max(tMin, near);
            tMax = Math.min(tMax, far);
            if (tMin > tMax) {
                return null;
            }
        }

        double distanceSquared = (dx * dx + dy * dy + dz * dz) * tMin * tMin;
        if (distanceSquared > maxDistanceSquared) {
            return null;
        }
        return new BenchRayHit(index, distanceSquared);
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

    private record BenchEntityIndex(BenchEntity[] cells, int side, int count) {
        void collect(AABB box, List<BenchEntity> output) {
            int minX = clampCell(box.minX);
            int maxX = clampCell(box.maxX);
            int minZ = clampCell(box.minZ);
            int maxZ = clampCell(box.maxZ);
            for (int z = minZ; z <= maxZ; z++) {
                int row = z * side;
                for (int x = minX; x <= maxX; x++) {
                    int index = row + x;
                    if (index >= count) {
                        continue;
                    }
                    var entity = cells[index];
                    if (entity != null && entity.intersects(box)) {
                        output.add(entity);
                    }
                }
            }
        }

        int count(AABB box) {
            int found = 0;
            int minX = clampCell(box.minX);
            int maxX = clampCell(box.maxX);
            int minZ = clampCell(box.minZ);
            int maxZ = clampCell(box.maxZ);
            for (int z = minZ; z <= maxZ; z++) {
                int row = z * side;
                for (int x = minX; x <= maxX; x++) {
                    int index = row + x;
                    if (index >= count) {
                        continue;
                    }
                    var entity = cells[index];
                    if (entity != null && entity.intersects(box)) {
                        found++;
                    }
                }
            }
            return found;
        }

        BenchEntity first(AABB box) {
            int minX = clampCell(box.minX);
            int maxX = clampCell(box.maxX);
            int minZ = clampCell(box.minZ);
            int maxZ = clampCell(box.maxZ);
            for (int z = minZ; z <= maxZ; z++) {
                int row = z * side;
                for (int x = minX; x <= maxX; x++) {
                    int index = row + x;
                    if (index >= count) {
                        continue;
                    }
                    var entity = cells[index];
                    if (entity != null && entity.intersects(box)) {
                        return entity;
                    }
                }
            }
            return null;
        }

        void forEach(AABB box, java.util.function.Consumer<BenchEntity> action) {
            int minX = clampCell(box.minX);
            int maxX = clampCell(box.maxX);
            int minZ = clampCell(box.minZ);
            int maxZ = clampCell(box.maxZ);
            for (int z = minZ; z <= maxZ; z++) {
                int row = z * side;
                for (int x = minX; x <= maxX; x++) {
                    int index = row + x;
                    if (index >= count) {
                        continue;
                    }
                    var entity = cells[index];
                    if (entity != null && entity.intersects(box)) {
                        action.accept(entity);
                    }
                }
            }
        }

        private int clampCell(double coordinate) {
            int cell = (int) Math.floor(coordinate / ENTITY_SPACING);
            if (cell < 0) {
                return 0;
            }
            if (cell >= side) {
                return side - 1;
            }
            return cell;
        }
    }

    private record BenchBlockPos(int x, int y, int z) {
    }

    private static final class BenchRaySnapshot {
        private int[] indices;
        private double[] minX;
        private double[] minY;
        private double[] minZ;
        private double[] maxX;
        private double[] maxY;
        private double[] maxZ;
        private int size;

        private BenchRaySnapshot(int capacity) {
            this.indices = new int[capacity];
            this.minX = new double[capacity];
            this.minY = new double[capacity];
            this.minZ = new double[capacity];
            this.maxX = new double[capacity];
            this.maxY = new double[capacity];
            this.maxZ = new double[capacity];
        }

        private void add(int index, AABB box, double inflate) {
            ensureCapacity(size + 1);
            int offset = size++;
            indices[offset] = index;
            minX[offset] = box.minX - inflate;
            minY[offset] = box.minY - inflate;
            minZ[offset] = box.minZ - inflate;
            maxX[offset] = box.maxX + inflate;
            maxY[offset] = box.maxY + inflate;
            maxZ[offset] = box.maxZ + inflate;
        }

        private void ensureCapacity(int required) {
            if (required <= indices.length) {
                return;
            }
            int capacity = Math.max(required, indices.length * 2);
            indices = java.util.Arrays.copyOf(indices, capacity);
            minX = java.util.Arrays.copyOf(minX, capacity);
            minY = java.util.Arrays.copyOf(minY, capacity);
            minZ = java.util.Arrays.copyOf(minZ, capacity);
            maxX = java.util.Arrays.copyOf(maxX, capacity);
            maxY = java.util.Arrays.copyOf(maxY, capacity);
            maxZ = java.util.Arrays.copyOf(maxZ, capacity);
        }
    }

    private record BenchRayHit(int index, double distanceSquared) {
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
