package io.fand.server.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class ChunkTaskExecutorsTest {

    @Test
    void worldgenThreadCountDefaultsToAtLeastTwoAndUpToAvailableProcessors() {
        int processors = Runtime.getRuntime().availableProcessors();

        int worldgenThreads = ChunkTaskExecutors.worldgenThreadCount(0);

        assertThat(worldgenThreads).isBetween(2, Math.max(2, Math.min(32, processors)));
    }

    @Test
    void worldgenThreadCountUsesConfiguredValueWhenPositive() {
        assertThat(ChunkTaskExecutors.worldgenThreadCount(7)).isEqualTo(7);
    }
}
