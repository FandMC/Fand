package io.fand.server.redstone;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.core.BlockPos;

public final class RedstoneRegionManager {

    private static final long HOT_SAMPLE_THRESHOLD = 1024L;
    private static final long HOT_NANOS_THRESHOLD = 1_000_000L;

    private final ConcurrentHashMap<RedstoneRegionKey, RegionState> regions = new ConcurrentHashMap<>();

    public void observe(String level, long blockPos, long durationNanos, long sampleWeight) {
        if (sampleWeight <= 0L) {
            return;
        }
        var key = RedstoneRegionKey.fromBlock(level, blockPos);
        regions.computeIfAbsent(key, RegionState::new)
                .record(Math.max(0L, durationNanos), sampleWeight);
    }

    public void markBlockDirty(String level, long blockPos, String reason) {
        int chunkX = BlockPos.getX(blockPos) >> 4;
        int chunkZ = BlockPos.getZ(blockPos) >> 4;
        markChunkActivity(level, chunkX, chunkZ, reason);
    }

    public void markChunkActivity(String level, int chunkX, int chunkZ, String reason) {
        markRegionActivity(RedstoneRegionKey.fromChunk(level, chunkX, chunkZ), reason);
        int localX = Math.floorMod(chunkX, RedstoneRegionKey.REGION_SIZE_CHUNKS);
        int localZ = Math.floorMod(chunkZ, RedstoneRegionKey.REGION_SIZE_CHUNKS);
        if (localX == 0) {
            markRegionActivity(RedstoneRegionKey.fromChunk(level, chunkX - 1, chunkZ), reason);
        } else if (localX == RedstoneRegionKey.REGION_SIZE_CHUNKS - 1) {
            markRegionActivity(RedstoneRegionKey.fromChunk(level, chunkX + 1, chunkZ), reason);
        }
        if (localZ == 0) {
            markRegionActivity(RedstoneRegionKey.fromChunk(level, chunkX, chunkZ - 1), reason);
        } else if (localZ == RedstoneRegionKey.REGION_SIZE_CHUNKS - 1) {
            markRegionActivity(RedstoneRegionKey.fromChunk(level, chunkX, chunkZ + 1), reason);
        }
    }

    public void markChunkDirty(String level, int chunkX, int chunkZ, String reason) {
        markRegionDirty(RedstoneRegionKey.fromChunk(level, chunkX, chunkZ), reason);
        int localX = Math.floorMod(chunkX, RedstoneRegionKey.REGION_SIZE_CHUNKS);
        int localZ = Math.floorMod(chunkZ, RedstoneRegionKey.REGION_SIZE_CHUNKS);
        if (localX == 0) {
            markRegionDirty(RedstoneRegionKey.fromChunk(level, chunkX - 1, chunkZ), reason);
        } else if (localX == RedstoneRegionKey.REGION_SIZE_CHUNKS - 1) {
            markRegionDirty(RedstoneRegionKey.fromChunk(level, chunkX + 1, chunkZ), reason);
        }
        if (localZ == 0) {
            markRegionDirty(RedstoneRegionKey.fromChunk(level, chunkX, chunkZ - 1), reason);
        } else if (localZ == RedstoneRegionKey.REGION_SIZE_CHUNKS - 1) {
            markRegionDirty(RedstoneRegionKey.fromChunk(level, chunkX, chunkZ + 1), reason);
        }
    }

    public List<RedstoneRegionSnapshot> snapshot(int topLimit) {
        int limit = Math.max(0, topLimit);
        return regions.values().stream()
                .map(RegionState::snapshot)
                .sorted(Comparator.comparingLong(RedstoneRegionSnapshot::totalNanos).reversed())
                .limit(limit)
                .toList();
    }

    public List<RedstoneClusterCandidateSnapshot> candidates(
            List<RedstoneProbeSnapshot.ClusterEntry> clusters,
            int topLimit
    ) {
        int limit = Math.max(0, topLimit);
        if (limit == 0 || clusters.isEmpty()) {
            return List.of();
        }
        return clusters.stream()
                .map(this::candidate)
                .sorted(Comparator.comparingLong(RedstoneClusterCandidateSnapshot::totalNanos).reversed())
                .limit(limit)
                .toList();
    }

    public void clear() {
        regions.clear();
    }

    private RedstoneClusterCandidateSnapshot candidate(RedstoneProbeSnapshot.ClusterEntry cluster) {
        int minRegionX = Math.floorDiv(cluster.minChunkX(), RedstoneRegionKey.REGION_SIZE_CHUNKS);
        int minRegionZ = Math.floorDiv(cluster.minChunkZ(), RedstoneRegionKey.REGION_SIZE_CHUNKS);
        int maxRegionX = Math.floorDiv(cluster.maxChunkX(), RedstoneRegionKey.REGION_SIZE_CHUNKS);
        int maxRegionZ = Math.floorDiv(cluster.maxChunkZ(), RedstoneRegionKey.REGION_SIZE_CHUNKS);
        int coveredRegions = 0;
        int dirtyRegions = 0;
        long activityCount = 0L;
        long invalidationCount = 0L;
        for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
            for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                coveredRegions++;
                var region = regions.get(new RedstoneRegionKey(cluster.level(), regionX, regionZ));
                if (region == null) {
                    continue;
                }
                if (region.dirty()) {
                    dirtyRegions++;
                }
                activityCount += region.activityCount();
                invalidationCount += region.invalidationCount();
            }
        }
        boolean hot = cluster.count() >= HOT_SAMPLE_THRESHOLD || cluster.totalNanos() >= HOT_NANOS_THRESHOLD;
        boolean readyForShadow = hot && dirtyRegions == 0;
        return new RedstoneClusterCandidateSnapshot(
                cluster.level(),
                cluster.minChunkX(),
                cluster.minChunkZ(),
                cluster.maxChunkX(),
                cluster.maxChunkZ(),
                cluster.chunks(),
                coveredRegions,
                dirtyRegions,
                hot,
                readyForShadow,
                cluster.count(),
                cluster.totalNanos(),
                activityCount,
                invalidationCount,
                readyForShadow ? "ready" : blockedReason(hot, dirtyRegions));
    }

    private static String blockedReason(boolean hot, int dirtyRegions) {
        if (!hot) {
            return "not-hot";
        }
        if (dirtyRegions > 0) {
            return "dirty-region";
        }
        return "unknown";
    }

    private void markRegionDirty(RedstoneRegionKey key, String reason) {
        regions.computeIfAbsent(key, RegionState::new)
                .markDirty(reason);
    }

    private void markRegionActivity(RedstoneRegionKey key, String reason) {
        var region = regions.get(key);
        if (region != null) {
            region.markActivity(reason);
        }
    }

    private static final class RegionState {

        private final RedstoneRegionKey key;
        private final LongAdder samples = new LongAdder();
        private final LongAdder totalNanos = new LongAdder();
        private final LongAdder activityCount = new LongAdder();
        private final LongAdder invalidationCount = new LongAdder();
        private final AtomicLong generation = new AtomicLong();
        private final AtomicBoolean dirty = new AtomicBoolean();
        private volatile String activityReason = "";
        private volatile String invalidationReason = "";

        private RegionState(RedstoneRegionKey key) {
            this.key = key;
        }

        private void record(long durationNanos, long sampleWeight) {
            samples.add(sampleWeight);
            totalNanos.add(durationNanos);
        }

        private void markActivity(String reason) {
            activityReason = normalizeReason(reason);
            activityCount.increment();
        }

        private void markDirty(String reason) {
            invalidationReason = normalizeReason(reason);
            invalidationCount.increment();
            if (dirty.compareAndSet(false, true)) {
                generation.incrementAndGet();
            }
        }

        private RedstoneRegionSnapshot snapshot() {
            long sampleCount = samples.sum();
            long nanos = totalNanos.sum();
            return new RedstoneRegionSnapshot(
                    key.level(),
                    key.regionX(),
                    key.regionZ(),
                    key.minChunkX(),
                    key.minChunkZ(),
                    key.maxChunkX(),
                    key.maxChunkZ(),
                    sampleCount >= HOT_SAMPLE_THRESHOLD || nanos >= HOT_NANOS_THRESHOLD,
                    dirty.get(),
                    generation.get(),
                    sampleCount,
                    nanos,
                    activityCount.sum(),
                    activityReason,
                    invalidationCount.sum(),
                    invalidationReason);
        }

        private boolean dirty() {
            return dirty.get();
        }

        private long activityCount() {
            return activityCount.sum();
        }

        private long invalidationCount() {
            return invalidationCount.sum();
        }

        private static String normalizeReason(String reason) {
            return reason == null || reason.isBlank() ? "unknown" : reason;
        }
    }
}
