package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.block.BlockType;
import io.fand.api.component.DataComponentMap;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class BlockBatchModelsTest {

    private static final BlockType STONE = new TestBlockType(Key.key("minecraft:stone"));
    private static final BlockType AIR = new TestBlockType(Key.key("minecraft:air"));

    @Test
    void blockChangeDefaultsNullComponentsToEmptyMap() {
        var change = BlockBatchChange.of(1, 2, 3, STONE, null);

        assertThat(change.components()).isEqualTo(DataComponentMap.EMPTY);
        assertThat(change.offset(10, 20, 30)).isEqualTo(BlockBatchChange.of(11, 22, 33, STONE));
        assertThatThrownBy(() -> BlockBatchChange.of(Integer.MAX_VALUE, 0, 0, STONE).offset(1, 0, 0))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void blockOptionsValidatePositiveSliceSize() {
        assertThat(BlockBatchOptions.defaults().maxBlocksPerTick())
                .isEqualTo(BlockBatchOptions.DEFAULT_MAX_BLOCKS_PER_TICK);
        assertThat(BlockBatchOptions.withoutNeighborUpdates().updateMode()).isEqualTo(BlockUpdateMode.CLIENTS_ONLY);
        assertThat(BlockBatchOptions.immediate().maxBlocksPerTick()).isEqualTo(Integer.MAX_VALUE);
        assertThatThrownBy(() -> new BlockBatchOptions(0, BlockUpdateMode.NORMAL, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockResultRejectsImpossibleCounters() {
        assertThat(BlockBatchResult.empty()).isEqualTo(new BlockBatchResult(0, 0, 0, 0));
        assertThatThrownBy(() -> new BlockBatchResult(1, 1, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BlockBatchResult(Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clipboardKeepsRelativeBlocksWithinDeclaredSize() {
        var clipboard = BlockClipboard.of(2, 1, 2, List.of(
                BlockBatchChange.of(0, 0, 0, STONE),
                BlockBatchChange.of(1, 0, 1, AIR)));

        assertThat(clipboard.blocks()).hasSize(2);
        assertThat(clipboard.empty()).isFalse();
        assertThatThrownBy(() -> BlockClipboard.of(1, 1, 1, List.of(BlockBatchChange.of(1, 0, 0, STONE))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private record TestBlockType(Key key) implements BlockType {
    }
}
