package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class RegionizedChunkTaskSchedulerTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void mapsChunksInSameRegionToSameLane() {
        try (var scheduler = new RegionizedChunkTaskScheduler(2, "test")) {
            assertThat(scheduler.laneIndex(ChunkPos.pack(0, 0)))
                    .isEqualTo(scheduler.laneIndex(ChunkPos.pack(7, 7)));
        }
    }

    @Test
    void spreadsDifferentRegionsAcrossAvailableLanes() {
        var firstRegionChunk = ChunkPos.pack(0, 0);
        var nextRegionChunk = ChunkPos.pack(RegionizedChunkTaskScheduler.REGION_SIZE_CHUNKS, 0);

        try (var scheduler = new RegionizedChunkTaskScheduler(2, "test")) {
            assertThat(scheduler.laneIndex(nextRegionChunk)).isNotEqualTo(scheduler.laneIndex(firstRegionChunk));
            assertThat(scheduler.laneCount()).isEqualTo(2);
        }
    }
}
