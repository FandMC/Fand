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
        if (!mode.profilingEnabled()) {
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
        if (currentMode.profilingEnabled()) {
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

    public void clear() {
        profiler.clear();
        regions.clear();
        shadow.clear();
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
