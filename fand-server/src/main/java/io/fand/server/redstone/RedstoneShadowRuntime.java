package io.fand.server.redstone;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class RedstoneShadowRuntime {

    private static final long READY_OBSERVATIONS = 2L;

    private final ConcurrentHashMap<ShadowKey, ShadowState> candidates = new ConcurrentHashMap<>();

    public void observe(List<RedstoneClusterCandidateSnapshot> clusterCandidates) {
        for (var candidate : clusterCandidates) {
            candidates.computeIfAbsent(ShadowKey.of(candidate), ShadowState::new)
                    .observe(candidate);
        }
    }

    public List<RedstoneShadowCandidateSnapshot> snapshot(int topLimit) {
        int limit = Math.max(0, topLimit);
        if (limit == 0) {
            return List.of();
        }
        return candidates.values().stream()
                .map(ShadowState::snapshot)
                .sorted(Comparator.comparingLong(RedstoneShadowCandidateSnapshot::lastNanos).reversed())
                .limit(limit)
                .toList();
    }

    public void clear() {
        candidates.clear();
    }

    private record ShadowKey(String level, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {

        private static ShadowKey of(RedstoneClusterCandidateSnapshot candidate) {
            return new ShadowKey(
                    candidate.level(),
                    candidate.minChunkX(),
                    candidate.minChunkZ(),
                    candidate.maxChunkX(),
                    candidate.maxChunkZ());
        }
    }

    private static final class ShadowState {

        private final ShadowKey key;
        private final AtomicBoolean ready = new AtomicBoolean();
        private final AtomicLong stableObservations = new AtomicLong();
        private final AtomicLong blockedObservations = new AtomicLong();
        private final AtomicLong lastSamples = new AtomicLong();
        private final AtomicLong lastNanos = new AtomicLong();
        private volatile String lastReason = "";

        private ShadowState(ShadowKey key) {
            this.key = key;
        }

        private void observe(RedstoneClusterCandidateSnapshot candidate) {
            lastSamples.set(candidate.samples());
            lastNanos.set(candidate.totalNanos());
            lastReason = candidate.blockedReason();
            if (candidate.readyForShadow()) {
                long stable = stableObservations.incrementAndGet();
                ready.set(stable >= READY_OBSERVATIONS);
            } else {
                blockedObservations.incrementAndGet();
                stableObservations.set(0L);
                ready.set(false);
            }
        }

        private RedstoneShadowCandidateSnapshot snapshot() {
            return new RedstoneShadowCandidateSnapshot(
                    key.level(),
                    key.minChunkX(),
                    key.minChunkZ(),
                    key.maxChunkX(),
                    key.maxChunkZ(),
                    ready.get(),
                    stableObservations.get(),
                    blockedObservations.get(),
                    lastSamples.get(),
                    lastNanos.get(),
                    lastReason);
        }
    }
}
