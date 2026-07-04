package io.fand.server.redstone;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class RedstoneRuntime {

    private static final int SHADOW_REFRESH_TOP_LIMIT = 16;
    private static final long SHADOW_REFRESH_INTERVAL_SAMPLES = 65_536L;
    private static final int SAMPLE_SHIFT = 4;
    private static final long SAMPLE_INTERVAL = 1L << SAMPLE_SHIFT;

    private final RedstoneProfiler profiler = new RedstoneProfiler();
    private final RedstoneRegionManager regions = new RedstoneRegionManager();
    private final RedstoneShadowRuntime shadow = new RedstoneShadowRuntime();
    private final RedstoneWireJitExecutor wireJit = new RedstoneWireJitExecutor();
    private final AtomicLong observedEvents = new AtomicLong();
    private final AtomicLong sampledEventsSinceShadowRefresh = new AtomicLong();
    private volatile RedstoneJitMode mode = RedstoneJitMode.OFF;

    public RedstoneRuntime(RedstoneJitMode mode) {
        configure(mode);
    }

    public void configure(RedstoneJitMode newMode) {
        mode = Objects.requireNonNull(newMode, "newMode");
        if (!newMode.profilingEnabled()) {
            profiler.clear();
            regions.clear();
            shadow.clear();
            wireJit.clear();
            observedEvents.set(0L);
            sampledEventsSinceShadowRefresh.set(0L);
        } else if (!newMode.shadowEnabled()) {
            shadow.clear();
            sampledEventsSinceShadowRefresh.set(0L);
        }
    }

    public RedstoneJitMode mode() {
        return mode;
    }

    public boolean profilingEnabled() {
        return mode.profilingEnabled();
    }

    public long beginProbe() {
        if (!mode.probeEnabled()) {
            return 0L;
        }
        long event = observedEvents.incrementAndGet();
        if ((event & (SAMPLE_INTERVAL - 1L)) != 0L) {
            return 0L;
        }
        profiler.observe(SAMPLE_INTERVAL);
        return System.nanoTime();
    }

    public void recordSample(RedstoneProbeType type, String level, long blockPos, long startNanos) {
        if (startNanos == 0L) {
            return;
        }
        var currentMode = mode;
        if (currentMode.probeEnabled()) {
            long durationNanos = System.nanoTime() - startNanos;
            recordWeightedSample(type, level, blockPos, durationNanos, SAMPLE_INTERVAL);
            maybeRefreshShadowCandidates(currentMode);
        }
    }

    void recordForTest(RedstoneProbeType type, String level, long blockPos, long durationNanos) {
        if (profilingEnabled()) {
            profiler.observe(1L);
            recordWeightedSample(type, level, blockPos, durationNanos, 1L);
        }
    }

    public RedstoneProbeSnapshot snapshot(int topLimit) {
        return profiler.snapshot(mode, topLimit);
    }

    public RedstoneRegionManager regions() {
        return regions;
    }

    public List<RedstoneClusterCandidateSnapshot> clusterCandidates(RedstoneProbeSnapshot snapshot, int topLimit) {
        return regions.candidates(snapshot.topClusters(), topLimit);
    }

    public List<RedstoneClusterCandidateSnapshot> refreshShadowCandidates(int topLimit) {
        if (!mode.shadowEnabled()) {
            return List.of();
        }
        var snapshot = snapshot(topLimit);
        var candidates = clusterCandidates(snapshot, topLimit);
        shadow.observe(candidates);
        return candidates;
    }

    public List<RedstoneShadowCandidateSnapshot> shadowCandidates(int topLimit) {
        return shadow.snapshot(topLimit);
    }

    public boolean tryUpdateWirePower(
            net.minecraft.world.level.block.RedStoneWireBlock wireBlock,
            net.minecraft.world.level.Level world,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state
    ) {
        return mode.executorEnabled() && wireJit.tryUpdatePowerStrength(wireBlock, world, pos, state);
    }

    public RedstoneWireJitSnapshot wireJitSnapshot() {
        return wireJit.snapshot();
    }

    public void markBlockDirty(String level, net.minecraft.world.level.Level world, long blockPos, String reason) {
        regions.markBlockDirty(level, blockPos, reason);
        wireJit.invalidateBlock(world, blockPos);
    }

    public void markChunkDirty(String level, net.minecraft.world.level.Level world, int chunkX, int chunkZ, String reason) {
        regions.markChunkDirty(level, chunkX, chunkZ, reason);
        wireJit.invalidateChunk(world, chunkX, chunkZ);
    }

    public void clear() {
        profiler.clear();
        regions.clear();
        shadow.clear();
        wireJit.clear();
        observedEvents.set(0L);
        sampledEventsSinceShadowRefresh.set(0L);
    }

    private void maybeRefreshShadowCandidates(RedstoneJitMode currentMode) {
        if (!currentMode.shadowEnabled()) {
            return;
        }
        long samples = sampledEventsSinceShadowRefresh.addAndGet(SAMPLE_INTERVAL);
        if (samples < SHADOW_REFRESH_INTERVAL_SAMPLES) {
            return;
        }
        if (sampledEventsSinceShadowRefresh.compareAndSet(samples, 0L)) {
            refreshShadowCandidates(SHADOW_REFRESH_TOP_LIMIT);
        }
    }

    private void recordWeightedSample(RedstoneProbeType type, String level, long blockPos, long durationNanos, long sampleWeight) {
        profiler.recordSample(type, level, blockPos, durationNanos, sampleWeight);
        regions.observe(level, blockPos, durationNanos * sampleWeight, sampleWeight);
    }
}
