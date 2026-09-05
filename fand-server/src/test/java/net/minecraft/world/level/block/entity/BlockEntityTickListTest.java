package net.minecraft.world.level.block.entity;

import io.fand.server.tick.OwnershipCell;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlockEntityTickListTest {
    private static final OwnershipCell LEFT = new OwnershipCell(-1, 0);
    private static final OwnershipCell RIGHT = new OwnershipCell(1, 0);

    @Test
    void tickOrderRemainsRegistrationOrderAcrossCells() {
        var list = newList();
        var order = new ArrayList<Ticker>();
        var first = new Ticker(64);
        var second = new Ticker(-1);
        var third = new Ticker(65);
        for (var ticker : List.of(first, second, third)) {
            ticker.action = () -> order.add(ticker);
            list.add(ticker);
        }
        list.add(first);

        list.tick(true, pos -> true);

        assertThat(order).containsExactly(first, second, third);
        assertThat(list.cellSizes()).containsExactlyInAnyOrderEntriesOf(Map.of(LEFT, 1, RIGHT, 2));
    }

    @Test
    void addedTickersStayInTheirPendingCellsUntilTheNextPass() {
        var list = newList();
        var first = new Ticker(-1);
        var added = new Ticker(64);
        first.action = () -> {
            list.add(added);
            list.add(added);
            assertThat(list.pendingCellSizes()).containsExactlyEntriesOf(Map.of(RIGHT, 1));
        };
        list.add(first);
        list.tick(true, pos -> true);

        assertThat(added.ticks).isZero();
        assertThat(list.cellSizes()).containsExactlyEntriesOf(Map.of(LEFT, 1));
        assertThat(list.pendingCellSizes()).containsExactlyEntriesOf(Map.of(RIGHT, 1));
        first.action = () -> {};
        list.tick(true, pos -> true);
        assertThat(first.ticks).isEqualTo(2);
        assertThat(added.ticks).isEqualTo(1);
        assertThat(list.pendingCellSizes()).isEmpty();
        assertThat(list.size()).isEqualTo(2);
    }

    @Test
    void deregisteringALaterTickerSkipsItInTheCurrentSnapshot() {
        var list = newList();
        var first = new Ticker(-1);
        var removed = new Ticker(64);
        first.action = () -> list.remove(removed);
        list.add(first);
        list.add(removed);

        list.tick(true, pos -> true);

        assertThat(first.ticks).isEqualTo(1);
        assertThat(removed.ticks).isZero();
        assertThat(list.cellSizes()).containsExactlyEntriesOf(Map.of(LEFT, 1));
    }

    @Test
    void deregistrationAfterWrapperRebindingUsesItsRegisteredCell() {
        var list = newList();
        var ticker = new Ticker(64);
        list.add(ticker);
        ticker.pos = BlockPos.ZERO;
        ticker.removed = true;

        list.remove(ticker);

        assertThat(list).isEmpty();
        assertThat(list.cellSizes()).isEmpty();
    }

    @Test
    void pendingTickerCanBeUnloadedBeforeItsFirstTick() {
        var list = newList();
        var first = new Ticker(-1);
        var unloaded = new Ticker(64);
        first.action = () -> list.add(unloaded);
        list.add(first);
        list.tick(true, pos -> true);
        unloaded.pos = BlockPos.ZERO;
        list.remove(unloaded);
        first.action = () -> {};
        list.tick(true, pos -> true);

        assertThat(unloaded.ticks).isZero();
        assertThat(list.pendingCellSizes()).isEmpty();
        assertThat(list.cellSizes()).containsExactlyEntriesOf(Map.of(LEFT, 1));
    }

    @Test
    void replacementAtTheSamePositionStartsNextPassAndCannotBeRemovedByOldWrapper() {
        var list = newList();
        var first = new Ticker(-1);
        var old = new Ticker(64);
        var replacement = new Ticker(64);
        first.action = () -> {
            list.remove(old);
            list.add(replacement);
        };
        list.add(first);
        list.add(old);
        list.tick(true, pos -> true);
        assertThat(old.ticks).isZero();
        assertThat(replacement.ticks).isZero();

        first.action = () -> list.remove(old);
        list.tick(true, pos -> true);

        assertThat(replacement.ticks).isEqualTo(1);
        assertThat(list).containsExactly(first, replacement);
    }

    @Test
    void frozenTicksStillRetireRemovedEntriesWithoutCallingThePositionFilter() {
        var list = newList();
        var live = new Ticker(-1);
        var removed = new Ticker(64);
        removed.removed = true;
        list.add(live);
        list.add(removed);

        list.tick(false, pos -> { throw new AssertionError("frozen tick reached position filter"); });

        assertThat(live.ticks).isZero();
        assertThat(removed.ticks).isZero();
        assertThat(list.cellSizes()).containsExactlyEntriesOf(Map.of(LEFT, 1));
        list.tick(true, pos -> pos.getX() >= 0);
        assertThat(live.ticks).isZero();
        list.tick(true, pos -> true);
        assertThat(live.ticks).isEqualTo(1);
    }

    @Test
    void failureRestoresTickStateAndKeepsDeferredAdditions() {
        var list = newList();
        var first = new Ticker(-1);
        var deferred = new Ticker(64);
        var afterFailure = new Ticker(65);
        var failure = new IllegalStateException("tick failed");
        first.action = () -> {
            list.add(deferred);
            throw failure;
        };
        list.add(first);

        assertThatThrownBy(() -> list.tick(true, pos -> true)).isSameAs(failure);
        list.remove(first);
        list.add(afterFailure);
        assertThat(list).containsExactly(afterFailure);
        list.tick(true, pos -> true);

        assertThat(list).containsExactly(afterFailure, deferred);
        assertThat(deferred.ticks).isEqualTo(1);
        assertThat(afterFailure.ticks).isEqualTo(1);
        assertThat(list.pendingCellSizes()).isEmpty();
    }

    @Test
    void diagnosticIterationDuringATickDoesNotRunTheSimulationAgain() {
        var list = newList();
        var first = new Ticker(-1);
        var second = new Ticker(64);
        first.action = () -> {
            assertThat(list).containsExactly(first, second);
            assertThatThrownBy(() -> list.tick(true, pos -> true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("reentered");
        };
        list.add(first);
        list.add(second);

        list.tick(true, pos -> true);

        assertThat(first.ticks).isEqualTo(1);
        assertThat(second.ticks).isEqualTo(1);
    }

    private static BlockEntityTickList newList() {
        return new BlockEntityTickList(pos -> new OwnershipCell(Math.floorDiv(pos.getX(), 64), Math.floorDiv(pos.getZ(), 64)));
    }

    private static final class Ticker implements TickingBlockEntity {
        private BlockPos pos;
        private boolean removed;
        private int ticks;
        private Runnable action = () -> {};

        private Ticker(int x) {
            this.pos = new BlockPos(x, 64, 0);
        }

        @Override
        public void tick() {
            this.ticks++;
            this.action.run();
        }

        @Override
        public boolean isRemoved() {
            return this.removed;
        }

        @Override
        public BlockPos getPos() {
            return this.pos;
        }

        @Override
        public String getType() {
            return "test";
        }
    }
}
