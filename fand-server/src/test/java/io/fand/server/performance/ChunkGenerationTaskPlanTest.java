package io.fand.server.performance;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

final class ChunkGenerationTaskPlanTest {

    @Test
    void chunkGenerationTaskClassInitializes() {
        assertThatCode(() -> Class.forName("net.minecraft.server.level.ChunkGenerationTask"))
                .doesNotThrowAnyException();
    }
}
