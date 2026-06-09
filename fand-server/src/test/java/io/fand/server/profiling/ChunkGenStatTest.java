package io.fand.server.profiling;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.time.Duration;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.profiling.jfr.event.ChunkGenerationEvent;
import net.minecraft.util.profiling.jfr.stats.ChunkGenStat;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ChunkGenStatTest {

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void preservesChunkZWhenParsingRecordedChunkGenerationEvent() throws Exception {
        var chunkPos = new ChunkPos(7, -11);
        var event = new ChunkGenerationEvent(chunkPos, Level.OVERWORLD, "full");
        var recordingPath = Files.createTempFile("chunk-generation", ".jfr");

        try (var recording = new Recording()) {
            recording.enable(ChunkGenerationEvent.EVENT_NAME).withThreshold(Duration.ZERO);
            recording.start();
            event.begin();
            event.commit();
            recording.stop();
            recording.dump(recordingPath);
        }

        try (var recordingFile = new RecordingFile(recordingPath)) {
            var recordedEvent = recordingFile.readEvent();
            var stat = ChunkGenStat.from(recordedEvent);

            assertThat(stat.chunkPos()).isEqualTo(chunkPos);
            assertThat(stat.worldPos().x()).isEqualTo(chunkPos.getMinBlockX());
            assertThat(stat.worldPos().z()).isEqualTo(chunkPos.getMinBlockZ());
            assertThat(stat.status()).isSameAs(ChunkStatus.FULL);
            assertThat(stat.level()).isEqualTo("minecraft:overworld");
        } finally {
            Files.deleteIfExists(recordingPath);
        }
    }
}
