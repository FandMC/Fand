package io.fand.server.world;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.world.ChunkBatchOptions;
import io.fand.api.world.ChunkOrder;
import io.fand.api.world.ChunkPos;
import io.fand.api.world.Vector3;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ChunkBatchPlannerTest {

    @Test
    void sourceOrderDeduplicatesByDefault() {
        var chunks = List.of(
                ChunkPos.of(2, 0),
                ChunkPos.of(1, 0),
                ChunkPos.of(2, 0),
                ChunkPos.of(0, 0));

        assertThat(ChunkBatchPlanner.ordered(chunks, ChunkBatchOptions.defaults()))
                .containsExactly(ChunkPos.of(2, 0), ChunkPos.of(1, 0), ChunkPos.of(0, 0));
    }

    @Test
    void sourceOrderCanPreserveDuplicates() {
        var chunks = List.of(ChunkPos.of(1, 0), ChunkPos.of(1, 0));

        assertThat(ChunkBatchPlanner.ordered(chunks, ChunkBatchOptions.defaults().withDeduplicate(false)))
                .containsExactly(ChunkPos.of(1, 0), ChunkPos.of(1, 0));
    }

    @Test
    void nearestFirstUsesPriorityCenter() {
        var options = ChunkBatchOptions.defaults()
                .withOrder(ChunkOrder.NEAREST_FIRST)
                .withPriorityCenter(ChunkPos.of(0, 0));
        var chunks = List.of(ChunkPos.of(3, 0), ChunkPos.of(1, 0), ChunkPos.of(0, 2));

        assertThat(ChunkBatchPlanner.ordered(chunks, options))
                .containsExactly(ChunkPos.of(1, 0), ChunkPos.of(0, 2), ChunkPos.of(3, 0));
    }

    @Test
    void forwardFirstPrefersFacingSideWithinRing() {
        var options = ChunkBatchOptions.defaults()
                .withOrder(ChunkOrder.FORWARD_FIRST)
                .withPriorityCenter(ChunkPos.of(0, 0))
                .withPriorityDirection(new Vector3(0.0D, 0.0D, 1.0D));
        var chunks = List.of(
                ChunkPos.of(0, -1),
                ChunkPos.of(1, 0),
                ChunkPos.of(0, 1),
                ChunkPos.of(-1, 0));

        assertThat(ChunkBatchPlanner.ordered(chunks, options))
                .containsExactly(
                        ChunkPos.of(0, 1),
                        ChunkPos.of(-1, 0),
                        ChunkPos.of(1, 0),
                        ChunkPos.of(0, -1));
    }
}
