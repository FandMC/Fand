package io.fand.server.profiling;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.time.Duration;
import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;
import jdk.jfr.consumer.RecordingFile;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.profiling.jfr.stats.StructureGenStat;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class StructureGenStatTest {

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void preservesChunkZWhenParsingRecordedStructureGenerationEvent() throws Exception {
        var chunkPos = new ChunkPos(12, -9);
        var recordingPath = Files.createTempFile("structure-generation", ".jfr");

        try (var recording = new Recording()) {
            recording.enable(TestStructureGenerationEvent.EVENT_NAME).withThreshold(Duration.ZERO);
            recording.start();
            var event = new TestStructureGenerationEvent(chunkPos, "minecraft:village", "minecraft:overworld", true);
            event.begin();
            event.commit();
            recording.stop();
            recording.dump(recordingPath);
        }

        try (var recordingFile = new RecordingFile(recordingPath)) {
            var recordedEvent = recordingFile.readEvent();
            var stat = StructureGenStat.from(recordedEvent);

            assertThat(stat.chunkPos()).isEqualTo(chunkPos);
            assertThat(stat.structureName()).isEqualTo("minecraft:village");
            assertThat(stat.level()).isEqualTo("minecraft:overworld");
            assertThat(stat.success()).isTrue();
        } finally {
            Files.deleteIfExists(recordingPath);
        }
    }

    @Name(TestStructureGenerationEvent.EVENT_NAME)
    @Label("Structure Generation")
    @Category({"Minecraft", "World Generation"})
    @StackTrace(false)
    @Enabled(false)
    private static final class TestStructureGenerationEvent extends Event {
        private static final String EVENT_NAME = "minecraft.StructureGeneration";

        @Name("chunkPosX")
        public final int chunkPosX;
        @Name("chunkPosZ")
        public final int chunkPosZ;
        @Name("structure")
        public final String structure;
        @Name("level")
        public final String level;
        @Name("success")
        public final boolean success;

        private TestStructureGenerationEvent(ChunkPos chunkPos, String structure, String level, boolean success) {
            this.chunkPosX = chunkPos.x();
            this.chunkPosZ = chunkPos.z();
            this.structure = structure;
            this.level = level;
            this.success = success;
        }
    }
}
