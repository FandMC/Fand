package net.minecraft.world.level.chunk.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import io.fand.server.config.FandConfig;
import io.fand.server.hooks.FandHooks;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class IOWorkerBlendingTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void resetFandHooks() {
        var config = new FandConfig();
        FandHooks.applyPerformanceConfig(config.performance);
    }

    @Test
    void missingChunkLoadDoesNotCreateRegionFile() throws Exception {
        try (var worker = new IOWorker(new RegionStorageInfo("test", Level.OVERWORLD, "chunk"), tempDir, false)) {
            assertThat(worker.loadAsync(ChunkPos.ZERO).join()).isEmpty();
        }

        try (var files = Files.list(tempDir)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void oldChunkScanDoesNotCreateMissingRegionFiles() throws Exception {
        try (var worker = new IOWorker(new RegionStorageInfo("test", Level.OVERWORLD, "chunk"), tempDir, false)) {
            assertThat(worker.isOldChunkAround(ChunkPos.ZERO, 8)).isFalse();
        }

        try (var files = Files.list(tempDir)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void disablingRegionScanFastPathUsesVanillaMissingRegionBehaviour() throws Exception {
        var config = new FandConfig();
        config.performance.chunkStorageRegionScanFastPath = false;
        FandHooks.applyPerformanceConfig(config.performance);

        try (var worker = new IOWorker(new RegionStorageInfo("test", Level.OVERWORLD, "chunk"), tempDir, false)) {
            assertThat(worker.loadAsync(ChunkPos.ZERO).join()).isEmpty();
        }

        assertThat(Files.exists(tempDir.resolve("r.0.0.mca"))).isTrue();
    }

    @Test
    void oldChunkScanStillSeesPendingWrites() throws Exception {
        ChunkPos pos = ChunkPos.ZERO;
        CompoundTag oldChunk = new CompoundTag();
        oldChunk.putInt("DataVersion", 1);

        try (var worker = new IOWorker(new RegionStorageInfo("test", Level.OVERWORLD, "chunk"), tempDir, false)) {
            var write = worker.store(pos, oldChunk);

            assertThat(worker.isOldChunkAround(pos, 0)).isTrue();

            write.join();
        }
    }
}
