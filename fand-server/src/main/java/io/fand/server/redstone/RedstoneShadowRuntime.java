package io.fand.server.redstone;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class RedstoneShadowRuntime {

    private static final long READY_OBSERVATIONS = 2L;
    private static final long STALE_OBSERVATIONS = 3L;

    private final ConcurrentHashMap<ShadowKey, ShadowState> candidates = new ConcurrentHashMap<>();
    private final AtomicLong observationEpoch = new AtomicLong();

    public void observe(List<RedstoneClusterCandidateSnapshot> clusterCandidates) {
        long epoch = observationEpoch.incrementAndGet();
        var seen = new HashSet<ShadowKey>();
        for (var candidate : clusterCandidates) {
            var key = ShadowKey.of(candidate);
            seen.add(key);
            candidates.computeIfAbsent(key, ShadowState::new)
                    .observe(candidate, epoch);
        }
        candidates.entrySet().removeIf(entry -> !seen.contains(entry.getKey()) && entry.getValue().stale(epoch));
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
        private final AtomicLong lastObservedEpoch = new AtomicLong();
        private final AtomicLong lastSamples = new AtomicLong();
        private final AtomicLong lastNanos = new AtomicLong();
        private volatile String lastReason = "";

        private ShadowState(ShadowKey key) {
            this.key = key;
        }

        private void observe(RedstoneClusterCandidateSnapshot candidate, long epoch) {
            lastObservedEpoch.set(epoch);
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

        private boolean stale(long currentEpoch) {
            long missed = currentEpoch - lastObservedEpoch.get();
            if (missed < STALE_OBSERVATIONS) {
                return false;
            }
            ready.set(false);
            stableObservations.set(0L);
            return true;
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
