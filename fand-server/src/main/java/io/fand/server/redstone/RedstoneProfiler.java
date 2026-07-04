package io.fand.server.redstone;

import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.core.BlockPos;

public final class RedstoneProfiler {

    private static final int MAX_POSITION_SAMPLES = 8192;
    private static final long HOT_CLUSTER_MIN_NANOS = 1_000_000L;

    private final EnumMapCounters<RedstoneProbeType> typeCounters = new EnumMapCounters<>(RedstoneProbeType.class);
    private final ConcurrentHashMap<RedstoneRegionKey, Counter> regionCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ChunkKey, Counter> chunkCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<RedstoneProbeKey, Counter> positionCounters = new ConcurrentHashMap<>();
    private final LongAdder droppedPositionSamples = new LongAdder();

    public void record(RedstoneProbeType type, String level, long blockPos, long durationNanos) {
        var normalizedDuration = Math.max(0L, durationNanos);
        var normalizedLevel = level == null ? "unknown" : level;
        typeCounters.counter(type).add(normalizedDuration);

        int chunkX = BlockPos.getX(blockPos) >> 4;
        int chunkZ = BlockPos.getZ(blockPos) >> 4;
        chunkCounters.computeIfAbsent(new ChunkKey(normalizedLevel, chunkX, chunkZ), ignored -> new Counter())
                .add(normalizedDuration);
        regionCounters.computeIfAbsent(RedstoneRegionKey.fromChunk(normalizedLevel, chunkX, chunkZ), ignored -> new Counter())
                .add(normalizedDuration);

        var key = new RedstoneProbeKey(type, normalizedLevel, blockPos);
        var counter = positionCounters.get(key);
        if (counter == null) {
            if (positionCounters.size() >= MAX_POSITION_SAMPLES) {
                droppedPositionSamples.increment();
                return;
            }
            counter = positionCounters.computeIfAbsent(key, ignored -> new Counter());
        }
        counter.add(normalizedDuration);
    }

    public RedstoneProbeSnapshot snapshot(RedstoneJitMode mode, int topLimit) {
        var types = typeCounters.entries().stream()
                .map(entry -> new RedstoneProbeSnapshot.TypeEntry(entry.type(), entry.count(), entry.totalNanos()))
                .filter(entry -> entry.count() > 0L)
                .sorted(Comparator.comparingLong(RedstoneProbeSnapshot.TypeEntry::totalNanos).reversed())
                .toList();
        long totalCount = 0L;
        long totalNanos = 0L;
        for (var type : types) {
            totalCount += type.count();
            totalNanos += type.totalNanos();
        }
        int limit = Math.max(0, topLimit);
        var clusters = clusterHotChunks(limit);
        var regions = regionCounters.entrySet().stream()
                .map(entry -> {
                    var key = entry.getKey();
                    var counter = entry.getValue();
                    return new RedstoneProbeSnapshot.RegionEntry(
                            key.level(),
                            key.regionX(),
                            key.regionZ(),
                            key.minChunkX(),
                            key.minChunkZ(),
                            key.maxChunkX(),
                            key.maxChunkZ(),
                            counter.count(),
                            counter.totalNanos());
                })
                .sorted(Comparator.comparingLong(RedstoneProbeSnapshot.RegionEntry::totalNanos).reversed())
                .limit(limit)
                .toList();
        var chunks = chunkCounters.entrySet().stream()
                .map(entry -> {
                    var key = entry.getKey();
                    var counter = entry.getValue();
                    return new RedstoneProbeSnapshot.ChunkEntry(
                            key.level(),
                            key.chunkX(),
                            key.chunkZ(),
                            counter.count(),
                            counter.totalNanos());
                })
                .sorted(Comparator.comparingLong(RedstoneProbeSnapshot.ChunkEntry::totalNanos).reversed())
                .limit(limit)
                .toList();
        var positions = positionCounters.entrySet().stream()
                .map(entry -> {
                    var key = entry.getKey();
                    var counter = entry.getValue();
                    return new RedstoneProbeSnapshot.PositionEntry(
                            key.type(),
                            key.level(),
                            key.blockPos(),
                            counter.count(),
                            counter.totalNanos());
                })
                .sorted(Comparator.comparingLong(RedstoneProbeSnapshot.PositionEntry::totalNanos).reversed())
                .limit(limit)
                .toList();
        return new RedstoneProbeSnapshot(
                mode,
                totalCount,
                totalNanos,
                droppedPositionSamples.sum(),
                types,
                clusters,
                regions,
                chunks,
                positions);
    }

    public void clear() {
        typeCounters.clear();
        regionCounters.clear();
        chunkCounters.clear();
        positionCounters.clear();
        droppedPositionSamples.reset();
    }

    private List<RedstoneProbeSnapshot.ClusterEntry> clusterHotChunks(int topLimit) {
        if (topLimit <= 0 || chunkCounters.isEmpty()) {
            return List.of();
        }
        var chunks = new HashMap<ChunkKey, ChunkStat>();
        long maxNanos = 0L;
        for (var entry : chunkCounters.entrySet()) {
            var counter = entry.getValue();
            long count = counter.count();
            long totalNanos = counter.totalNanos();
            chunks.put(entry.getKey(), new ChunkStat(entry.getKey(), count, totalNanos));
            maxNanos = Math.max(maxNanos, totalNanos);
        }
        long minNanos = Math.max(HOT_CLUSTER_MIN_NANOS, maxNanos / 100L);
        var candidates = new HashMap<ChunkKey, ChunkStat>();
        for (var entry : chunks.entrySet()) {
            if (entry.getValue().totalNanos() >= minNanos) {
                candidates.put(entry.getKey(), entry.getValue());
            }
        }
        var visited = new HashSet<ChunkKey>();
        var clusters = new java.util.ArrayList<RedstoneProbeSnapshot.ClusterEntry>();
        for (var candidate : candidates.keySet()) {
            if (!visited.add(candidate)) {
                continue;
            }
            clusters.add(collectCluster(candidate, candidates, visited));
        }
        return clusters.stream()
                .sorted(Comparator.comparingLong(RedstoneProbeSnapshot.ClusterEntry::totalNanos).reversed())
                .limit(topLimit)
                .toList();
    }

    private static RedstoneProbeSnapshot.ClusterEntry collectCluster(
            ChunkKey start,
            HashMap<ChunkKey, ChunkStat> candidates,
            HashSet<ChunkKey> visited
    ) {
        var queue = new ArrayDeque<ChunkKey>();
        queue.add(start);
        int minChunkX = start.chunkX();
        int minChunkZ = start.chunkZ();
        int maxChunkX = start.chunkX();
        int maxChunkZ = start.chunkZ();
        int chunks = 0;
        long count = 0L;
        long totalNanos = 0L;
        while (!queue.isEmpty()) {
            var key = queue.removeFirst();
            var stat = candidates.get(key);
            if (stat == null) {
                continue;
            }
            chunks++;
            count += stat.count();
            totalNanos += stat.totalNanos();
            minChunkX = Math.min(minChunkX, key.chunkX());
            minChunkZ = Math.min(minChunkZ, key.chunkZ());
            maxChunkX = Math.max(maxChunkX, key.chunkX());
            maxChunkZ = Math.max(maxChunkZ, key.chunkZ());
            addNeighbor(key, 1, 0, candidates, visited, queue);
            addNeighbor(key, -1, 0, candidates, visited, queue);
            addNeighbor(key, 0, 1, candidates, visited, queue);
            addNeighbor(key, 0, -1, candidates, visited, queue);
        }
        return new RedstoneProbeSnapshot.ClusterEntry(
                start.level(),
                minChunkX,
                minChunkZ,
                maxChunkX,
                maxChunkZ,
                chunks,
                count,
                totalNanos);
    }

    private static void addNeighbor(
            ChunkKey key,
            int offsetX,
            int offsetZ,
            HashMap<ChunkKey, ChunkStat> candidates,
            HashSet<ChunkKey> visited,
            ArrayDeque<ChunkKey> queue
    ) {
        var neighbor = new ChunkKey(key.level(), key.chunkX() + offsetX, key.chunkZ() + offsetZ);
        if (candidates.containsKey(neighbor) && visited.add(neighbor)) {
            queue.add(neighbor);
        }
    }

    private static final class EnumMapCounters<E extends Enum<E>> {

        private final Counter[] counters;
        private final E[] values;

        private EnumMapCounters(Class<E> enumType) {
            this.values = enumType.getEnumConstants();
            this.counters = new Counter[values.length];
            for (int i = 0; i < counters.length; i++) {
                counters[i] = new Counter();
            }
        }

        private Counter counter(E value) {
            return counters[value.ordinal()];
        }

        private List<Entry<E>> entries() {
            return java.util.stream.IntStream.range(0, values.length)
                    .mapToObj(index -> new Entry<>(
                            values[index],
                            counters[index].count(),
                            counters[index].totalNanos()))
                    .toList();
        }

        private void clear() {
            for (var counter : counters) {
                counter.clear();
            }
        }
    }

    private record Entry<E>(E type, long count, long totalNanos) {
    }

    private record ChunkKey(String level, int chunkX, int chunkZ) {
    }

    private record ChunkStat(ChunkKey key, long count, long totalNanos) {
    }

    private static final class Counter {

        private final LongAdder count = new LongAdder();
        private final LongAdder totalNanos = new LongAdder();

        private void add(long durationNanos) {
            count.increment();
            totalNanos.add(durationNanos);
        }

        private long count() {
            return count.sum();
        }

        private long totalNanos() {
            return totalNanos.sum();
        }

        private void clear() {
            count.reset();
            totalNanos.reset();
        }
    }
}
