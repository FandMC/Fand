package io.fand.server.redstone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.server.config.ConfigException;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

final class RedstoneRuntimeTest {

    @Test
    void parsesSupportedModes() {
        assertThat(RedstoneJitMode.fromConfig("off")).isEqualTo(RedstoneJitMode.OFF);
        assertThat(RedstoneJitMode.fromConfig("profile")).isEqualTo(RedstoneJitMode.PROFILE);
        assertThat(RedstoneJitMode.fromConfig("shadow")).isEqualTo(RedstoneJitMode.SHADOW);
        assertThat(RedstoneJitMode.fromConfig("interpreter")).isEqualTo(RedstoneJitMode.INTERPRETER);
        assertThat(RedstoneJitMode.fromConfig("hot")).isEqualTo(RedstoneJitMode.HOT);
        assertThat(RedstoneJitMode.fromConfig(" false ")).isEqualTo(RedstoneJitMode.OFF);
    }

    @Test
    void rejectsUnsupportedMode() {
        assertThatThrownBy(() -> RedstoneJitMode.fromConfig("turbo"))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("performance.redstoneJitMode");
    }

    @Test
    void recordsSamplesOnlyWhenProfilingEnabled() {
        var runtime = new RedstoneRuntime(RedstoneJitMode.OFF);

        recordSample(runtime, RedstoneProbeType.NEIGHBOR_UPDATE, "minecraft:overworld", 1L, 100L);
        assertThat(runtime.snapshot(10).totalCount()).isZero();

        runtime.configure(RedstoneJitMode.PROFILE);
        recordSample(runtime, RedstoneProbeType.NEIGHBOR_UPDATE, "minecraft:overworld", 1L, 100L);
        recordSample(runtime, RedstoneProbeType.NEIGHBOR_UPDATE, "minecraft:overworld", 1L, 200L);
        recordSample(runtime, RedstoneProbeType.PISTON_MOVE, "minecraft:the_nether", 2L, 300L);

        var snapshot = runtime.snapshot(10);
        assertThat(snapshot.totalCount()).isEqualTo(3L);
        assertThat(snapshot.totalNanos()).isEqualTo(600L);
        assertThat(snapshot.types()).extracting(RedstoneProbeSnapshot.TypeEntry::type)
                .containsExactlyInAnyOrder(RedstoneProbeType.PISTON_MOVE, RedstoneProbeType.NEIGHBOR_UPDATE);
        assertThat(snapshot.topRegions()).hasSize(2);
        assertThat(snapshot.topChunks()).hasSize(2);
        assertThat(snapshot.topPositions()).hasSize(2);
        assertThat(snapshot.topPositions()).extracting(RedstoneProbeSnapshot.PositionEntry::type)
                .containsExactlyInAnyOrder(RedstoneProbeType.PISTON_MOVE, RedstoneProbeType.NEIGHBOR_UPDATE);

        runtime.configure(RedstoneJitMode.OFF);
        assertThat(runtime.snapshot(10).totalCount()).isZero();
    }

    @Test
    void hotModeExecutesWithoutProfilerProbe() {
        var runtime = new RedstoneRuntime(RedstoneJitMode.HOT);

        assertThat(runtime.beginProbe()).isZero();
        assertThat(runtime.snapshot(10).observedEvents()).isZero();
    }

    @Test
    void keepsChunkAndRegionTotalsWhenPositionDetailsAreFull() {
        var runtime = new RedstoneRuntime(RedstoneJitMode.PROFILE);
        int samples = 9000;

        for (int index = 0; index < samples; index++) {
            recordSample(runtime,
                    RedstoneProbeType.NEIGHBOR_UPDATE,
                    "minecraft:overworld",
                    BlockPos.asLong(index << 4, 64, 0),
                    1L);
        }

        var snapshot = runtime.snapshot(5);
        assertThat(snapshot.totalCount()).isEqualTo(samples);
        assertThat(snapshot.totalNanos()).isEqualTo(samples);
        assertThat(snapshot.droppedPositionSamples()).isEqualTo(samples - 8192L);
        assertThat(snapshot.topPositions()).hasSize(5);
        assertThat(snapshot.topChunks()).hasSize(5);
        assertThat(snapshot.topRegions()).hasSize(5);
        assertThat(snapshot.topChunks()).extracting(RedstoneProbeSnapshot.ChunkEntry::count)
                .containsOnly(1L);
        assertThat(snapshot.topRegions().stream().mapToLong(RedstoneProbeSnapshot.RegionEntry::count).sum())
                .isEqualTo(40L);
    }

    @Test
    void keepsAggregatesAfterPositionSamplesAreFull() {
        var runtime = new RedstoneRuntime(RedstoneJitMode.PROFILE);
        int samples = 10_000;

        for (int index = 0; index < samples; index++) {
            recordSample(runtime,
                    RedstoneProbeType.WIRE_POWER_UPDATE,
                    "minecraft:overworld",
                    BlockPos.asLong(index << 4, 64, 0),
                    2L);
        }

        var snapshot = runtime.snapshot(5);
        assertThat(snapshot.totalCount()).isEqualTo(samples);
        assertThat(snapshot.totalNanos()).isEqualTo(samples * 2L);
        assertThat(snapshot.droppedPositionSamples()).isEqualTo(samples - 8192L);
        assertThat(snapshot.topPositions()).hasSize(5);
        assertThat(snapshot.topChunks().stream().mapToLong(RedstoneProbeSnapshot.ChunkEntry::count).sum())
                .isEqualTo(5L);
    }

    @Test
    void groupsAdjacentHotChunksIntoClusters() {
        var runtime = new RedstoneRuntime(RedstoneJitMode.PROFILE);

        recordChunk(runtime, 6, 1, 1300L);
        recordChunk(runtime, 7, 1, 1400L);
        recordChunk(runtime, 8, 1, 1500L);
        recordChunk(runtime, 8, 2, 1600L);
        recordChunk(runtime, 20, 20, 1700L);

        var clusters = runtime.snapshot(10).topClusters();
        assertThat(clusters).hasSize(2);
        var machineCluster = findCandidate(runtime.clusterCandidates(runtime.snapshot(10), 10), 6, 1, 8, 2);
        assertThat(machineCluster.chunks()).isEqualTo(4);
        assertThat(machineCluster.samples()).isEqualTo(4L);
        assertThat(machineCluster.totalNanos()).isEqualTo(5_800_000L);

        var candidates = runtime.clusterCandidates(runtime.snapshot(10), 10);
        assertThat(candidates).hasSize(2);
        machineCluster = findCandidate(candidates, 6, 1, 8, 2);
        assertThat(machineCluster.coveredRegions()).isEqualTo(2);
        assertThat(machineCluster.dirtyRegions()).isZero();
        assertThat(machineCluster.readyForShadow()).isTrue();
        assertThat(machineCluster.blockedReason()).isEqualTo("ready");

        runtime.regions().markChunkDirty("minecraft:overworld", 8, 1, "chunk-unload");

        candidates = runtime.clusterCandidates(runtime.snapshot(10), 10);
        machineCluster = findCandidate(candidates, 6, 1, 8, 2);
        assertThat(machineCluster.dirtyRegions()).isEqualTo(2);
        assertThat(machineCluster.readyForShadow()).isFalse();
        assertThat(machineCluster.blockedReason()).isEqualTo("dirty-region");
    }

    @Test
    void tracksShadowCandidateStabilityOnlyWhenShadowModeIsEnabled() {
        var runtime = new RedstoneRuntime(RedstoneJitMode.PROFILE);

        recordChunk(runtime, 2, 1, 1300L);
        var snapshot = runtime.snapshot(10);
        assertThat(runtime.clusterCandidates(snapshot, 10).getFirst().readyForShadow()).isTrue();
        assertThat(runtime.shadowCandidates(10)).isEmpty();

        runtime.configure(RedstoneJitMode.SHADOW);
        snapshot = runtime.snapshot(10);
        runtime.clusterCandidates(snapshot, 10);
        assertThat(runtime.shadowCandidates(10)).isEmpty();

        runtime.refreshShadowCandidates(10);
        var shadow = runtime.shadowCandidates(10);
        assertThat(shadow).hasSize(1);
        assertThat(shadow.getFirst().ready()).isFalse();
        assertThat(shadow.getFirst().stableObservations()).isEqualTo(1L);

        runtime.refreshShadowCandidates(10);
        shadow = runtime.shadowCandidates(10);
        assertThat(shadow.getFirst().ready()).isTrue();
        assertThat(shadow.getFirst().stableObservations()).isEqualTo(2L);

        runtime.regions().markChunkDirty("minecraft:overworld", 2, 1, "chunk-unload");
        runtime.refreshShadowCandidates(10);
        shadow = runtime.shadowCandidates(10);
        assertThat(shadow.getFirst().ready()).isFalse();
        assertThat(shadow.getFirst().stableObservations()).isZero();
        assertThat(shadow.getFirst().blockedObservations()).isEqualTo(1L);
        assertThat(shadow.getFirst().lastReason()).isEqualTo("dirty-region");
    }

    @Test
    void removesStaleShadowCandidates() {
        var shadow = new RedstoneShadowRuntime();
        var candidate = new RedstoneClusterCandidateSnapshot(
                "minecraft:overworld",
                2,
                1,
                5,
                2,
                8,
                1,
                0,
                true,
                true,
                2000L,
                2_000_000L,
                0L,
                0L,
                "ready");

        shadow.observe(java.util.List.of(candidate));
        shadow.observe(java.util.List.of(candidate));
        assertThat(shadow.snapshot(10).getFirst().ready()).isTrue();

        shadow.observe(java.util.List.of());
        shadow.observe(java.util.List.of());
        shadow.observe(java.util.List.of());
        assertThat(shadow.snapshot(10)).isEmpty();
    }

    @Test
    void tracksAndDirtiesObservedRegions() {
        var runtime = new RedstoneRuntime(RedstoneJitMode.PROFILE);

        recordSample(runtime, RedstoneProbeType.NEIGHBOR_UPDATE, "minecraft:overworld", BlockPos.asLong(120, 64, 16), 1_000_000L);
        runtime.regions().markBlockDirty("minecraft:overworld", BlockPos.asLong(120, 64, 16), "block-change");

        var regions = runtime.regions().snapshot(10);
        assertThat(regions).hasSize(1);
        assertThat(regions.getFirst().hot()).isTrue();
        assertThat(regions.getFirst().dirty()).isFalse();
        assertThat(regions.getFirst().generation()).isZero();
        assertThat(regions.getFirst().activityCount()).isEqualTo(1L);
        assertThat(regions.getFirst().activityReason()).isEqualTo("block-change");
        assertThat(regions.getFirst().invalidationCount()).isZero();

        runtime.regions().markChunkDirty("minecraft:overworld", 7, 1, "chunk-unload");
        runtime.regions().markChunkDirty("minecraft:overworld", 7, 1, "chunk-unload");

        regions = runtime.regions().snapshot(10);
        assertThat(regions.getFirst().dirty()).isTrue();
        assertThat(regions.getFirst().generation()).isEqualTo(1L);
        assertThat(regions.getFirst().invalidationCount()).isEqualTo(2L);
        assertThat(regions.getFirst().invalidationReason()).isEqualTo("chunk-unload");

        runtime.configure(RedstoneJitMode.OFF);
        assertThat(runtime.regions().snapshot(10)).isEmpty();
    }

    private static void recordChunk(RedstoneRuntime runtime, int chunkX, int chunkZ, long durationNanos) {
        recordSample(runtime,
                RedstoneProbeType.NEIGHBOR_UPDATE,
                "minecraft:overworld",
                BlockPos.asLong(chunkX << 4, 64, chunkZ << 4),
                durationNanos * 1000L);
    }

    private static void recordSample(
            RedstoneRuntime runtime,
            RedstoneProbeType type,
            String level,
            long blockPos,
            long durationNanos
    ) {
        runtime.recordForTest(type, level, blockPos, durationNanos);
    }

    private static RedstoneClusterCandidateSnapshot findCandidate(
            java.util.List<RedstoneClusterCandidateSnapshot> candidates,
            int minChunkX,
            int minChunkZ,
            int maxChunkX,
            int maxChunkZ
    ) {
        return candidates.stream()
                .filter(candidate -> candidate.minChunkX() == minChunkX
                        && candidate.minChunkZ() == minChunkZ
                        && candidate.maxChunkX() == maxChunkX
                        && candidate.maxChunkZ() == maxChunkZ)
                .findFirst()
                .orElseThrow();
    }
}
