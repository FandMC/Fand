package io.fand.server.redstone;

import java.util.List;
import java.util.Objects;

public final class RedstoneRuntime {

    private final RedstoneProfiler profiler = new RedstoneProfiler();
    private final RedstoneRegionManager regions = new RedstoneRegionManager();
    private final RedstoneShadowRuntime shadow = new RedstoneShadowRuntime();
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
        } else if (!newMode.shadowEnabled()) {
            shadow.clear();
        }
    }

    public RedstoneJitMode mode() {
        return mode;
    }

    public boolean profilingEnabled() {
        return mode.profilingEnabled();
    }

    public void record(RedstoneProbeType type, String level, long blockPos, long durationNanos) {
        if (profilingEnabled()) {
            profiler.record(type, level, blockPos, durationNanos);
            regions.observe(level, blockPos, durationNanos);
        }
    }

    public RedstoneProbeSnapshot snapshot(int topLimit) {
        return profiler.snapshot(mode, topLimit);
    }

    public RedstoneRegionManager regions() {
        return regions;
    }

    public List<RedstoneClusterCandidateSnapshot> clusterCandidates(RedstoneProbeSnapshot snapshot, int topLimit) {
        var candidates = regions.candidates(snapshot.topClusters(), topLimit);
        if (mode.shadowEnabled()) {
            shadow.observe(candidates);
        }
        return candidates;
    }

    public List<RedstoneShadowCandidateSnapshot> shadowCandidates(int topLimit) {
        return shadow.snapshot(topLimit);
    }

    public void clear() {
        profiler.clear();
        regions.clear();
        shadow.clear();
    }
}
