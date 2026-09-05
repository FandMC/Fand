package net.minecraft.world.level.redstone;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollectingNeighborUpdaterTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void nestedUpdatesKeepDepthFirstOrderAcrossCellBoundaries() {
        var harness = new Harness(-1);
        harness.action = pos -> {
            if (pos.getX() == 0) {
                harness.update(at(64));
                harness.update(at(128));
            } else if (pos.getX() == 64) {
                harness.update(at(65));
            }
        };

        harness.update(at(0));

        assertThat(harness.fired).containsExactly(at(0), at(64), at(65), at(128));
    }

    @Test
    void delayedSimpleUpdatesCaptureMutablePositions() {
        var harness = new Harness(-1);
        harness.action = pos -> {
            if (pos.getX() == 0) {
                var target = new BlockPos.MutableBlockPos(64, 64, 0);
                harness.update(target);
                target.set(128, 64, 0);
            }
        };

        harness.update(at(0));

        assertThat(harness.fired).containsExactly(at(0), at(64));
    }

    @Test
    void multiNeighborUpdatesResumeAfterChildrenInVanillaDirectionOrder() {
        var harness = new Harness(-1);
        BlockPos source = at(64);
        harness.action = pos -> {
            if (pos.equals(source.west())) {
                harness.update(at(128));
            }
        };

        harness.updater.updateNeighborsAtExceptFromFacing(source, Blocks.STONE, Direction.SOUTH, null);

        assertThat(harness.fired).containsExactly(source.west(), at(128), source.east(), source.below(), source.above(), source.north());
    }

    @Test
    void eachRootHasItsOwnChainBudget() {
        var harness = new Harness(2);
        harness.action = pos -> {
            if (pos.getX() == 0) {
                harness.update(at(64));
                harness.update(at(128));
            }
        };

        harness.update(at(0));
        harness.update(at(192));

        assertThat(harness.fired).containsExactly(at(0), at(64), at(192));
        var disabled = new Harness(0);
        disabled.update(at(0));
        assertThat(disabled.fired).isEmpty();
    }

    @Test
    void failuresDiscardOnlyTheFailingChain() {
        var harness = new Harness(-1);
        harness.action = pos -> {
            harness.update(at(64));
            throw new IllegalStateException("neighbor failed");
        };

        assertThatThrownBy(() -> harness.update(at(0))).isInstanceOf(ReportedException.class);
        harness.action = pos -> {};
        harness.update(at(128));

        assertThat(harness.fired).containsExactly(at(0), at(128));
    }

    @Test
    void unlimitedChainsDoNotConsumeTheJavaCallStack() {
        var harness = new Harness(-1);
        harness.action = pos -> {
            if (pos.getX() < 5000) {
                harness.update(at(pos.getX() + 1));
            }
        };

        harness.update(at(0));

        assertThat(harness.fired).hasSize(5001);
        assertThat(harness.fired.getLast()).isEqualTo(at(5000));
    }

    private static BlockPos at(int x) {
        return new BlockPos(x, 64, 0);
    }

    private static final class Harness {
        private final CollectingNeighborUpdater updater;
        private final List<BlockPos> fired = new ArrayList<>();
        private Consumer<BlockPos> action = pos -> {};

        private Harness(int limit) {
            Level level = mock(Level.class);
            BlockState state = mock(BlockState.class);
            when(level.getBlockState(any(BlockPos.class))).thenReturn(state);
            when(level.enabledFeatures()).thenReturn(FeatureFlagSet.of());
            doAnswer(invocation -> {
                BlockPos pos = invocation.getArgument(1);
                this.fired.add(pos.immutable());
                this.action.accept(pos);
                return null;
            }).when(state).handleNeighborChanged(eq(level), any(BlockPos.class), eq(Blocks.STONE), nullable(Orientation.class), anyBoolean());
            this.updater = new CollectingNeighborUpdater(level, limit);
        }

        private void update(BlockPos pos) {
            this.updater.neighborChanged(pos, Blocks.STONE, null);
        }
    }
}
