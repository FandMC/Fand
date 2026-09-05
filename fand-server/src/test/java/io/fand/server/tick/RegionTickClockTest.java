package io.fand.server.tick;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegionTickClockTest {
    private static final OwnershipCell LEFT = new OwnershipCell(0, 0);
    private static final OwnershipCell BRIDGE = new OwnershipCell(2, 0);
    private static final OwnershipCell RIGHT = new OwnershipCell(4, 0);

    @Test
    void localTimeAdvancesOnlyAfterSuccessfulSimulationAndLeaseRelease() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var region = topology.ownerOf(LEFT).orElseThrow();
            try (var context = topology.tryEnter(region).orElseThrow()) {
                assertThat(context.tickNumber()).isEqualTo(1);
                context.runTick(current -> assertThat(current.tickNumber()).isEqualTo(1));
                assertThat(region.completedTicks()).isZero();
                assertThatThrownBy(() -> context.runTick(ignored -> {})).isInstanceOf(IllegalStateException.class);
            }
            assertThat(region.completedTicks()).isEqualTo(1);
            try (var context = topology.tryEnter(region).orElseThrow()) {
                context.runTasks();
            }
            assertThat(region.completedTicks()).isEqualTo(1);
        }
    }

    @Test
    void delayedTasksWaitForSimulationTicksAndNotLeaseAcquisitions() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var delayed = topology.submitAfter(LEFT, 3, () -> 42);
            var region = topology.ownerOf(LEFT).orElseThrow();
            try (var context = topology.tryEnter(region).orElseThrow()) {
                assertThat(context.runTasks()).isZero();
            }
            tick(topology, LEFT, 2);
            assertThat(delayed).isNotDone();
            tick(topology, LEFT, 1);
            assertThat(delayed).isCompletedWithValue(42);
            assertThat(region.completedTicks()).isEqualTo(3);
        }
    }

    @Test
    void delaySubmittedInsideSimulationStartsAfterTheCurrentTick() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var delayed = new AtomicReference<CompletableFuture<Integer>>();
            var region = topology.ownerOf(LEFT).orElseThrow();
            try (var context = topology.tryEnter(region).orElseThrow()) {
                context.runTick(ignored -> delayed.set(topology.submitAfter(LEFT, 2, () -> 42)));
            }
            tick(topology, LEFT, 1);
            assertThat(delayed.get()).isNotDone();
            tick(topology, LEFT, 1);
            assertThat(delayed.get()).isCompletedWithValue(42);
        }
    }

    @Test
    void mergingDifferentClocksPreservesTheRemainingDelayForEachSource() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            topology.tryActivate(RIGHT);
            tick(topology, LEFT, 10);
            tick(topology, RIGHT, 2);
            var fromLeft = topology.submitAfter(LEFT, 4, () -> "left");
            var fromRight = topology.submitAfter(RIGHT, 3, () -> "right");
            tick(topology, LEFT, 1);
            tick(topology, RIGHT, 1);
            topology.tryActivate(BRIDGE);

            var merged = topology.ownerOf(BRIDGE).orElseThrow();
            assertThat(merged.completedTicks()).isEqualTo(11);
            tick(topology, BRIDGE, 1);
            assertThat(fromLeft).isNotDone();
            assertThat(fromRight).isNotDone();
            tick(topology, BRIDGE, 1);
            assertThat(fromRight).isCompletedWithValue("right");
            assertThat(fromLeft).isNotDone();
            tick(topology, BRIDGE, 1);
            assertThat(fromLeft).isCompletedWithValue("left");
        }
    }

    @Test
    void splitChildrenInheritTheClockAndAdvanceIndependently() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            topology.tryActivate(BRIDGE);
            topology.tryActivate(RIGHT);
            tick(topology, BRIDGE, 5);
            var leftTask = topology.submitAfter(LEFT, 2, () -> "left");
            var rightTask = topology.submitAfter(RIGHT, 2, () -> "right");
            topology.tryDeactivate(BRIDGE);

            assertThat(topology.regions()).allMatch(region -> region.completedTicks() == 5);
            tick(topology, LEFT, 2);
            assertThat(leftTask).isCompletedWithValue("left");
            assertThat(rightTask).isNotDone();
            assertThat(topology.ownerOf(RIGHT).orElseThrow().completedTicks()).isEqualTo(5);
            tick(topology, RIGHT, 2);
            assertThat(rightTask).isCompletedWithValue("right");
        }
    }

    @Test
    void retiringACellCancelsLongDelayedTasksAndDoesNotReviveThemOnReload() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var old = topology.submitAfter(LEFT, 1000, () -> "old");
            topology.tryDeactivate(LEFT);
            topology.tryActivate(LEFT);
            var fresh = topology.submitAfter(LEFT, 1, () -> "new");
            tick(topology, LEFT, 1);
            assertThat(old).isCancelled();
            assertThat(fresh).isCompletedWithValue("new");
        }
    }

    @Test
    void invalidAndOverflowingDelaysDoNotQueueAnAction() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            assertThatThrownBy(() -> topology.submitAfter(LEFT, 0, () -> 1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> topology.submitAfter(LEFT, -1, () -> 1)).isInstanceOf(IllegalArgumentException.class);
            tick(topology, LEFT, 1);
            assertThatThrownBy(() -> topology.submitAfter(LEFT, Long.MAX_VALUE, () -> 1))
                    .isInstanceOf(ArithmeticException.class);
            try (var context = topology.tryEnter(topology.ownerOf(LEFT).orElseThrow()).orElseThrow()) {
                assertThat(context.runTasks()).isZero();
            }
        }
    }

    @Test
    void deadlineOverflowDuringMergeLeavesBothOwnersAndTasksIntact() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            topology.tryActivate(RIGHT);
            tick(topology, LEFT, 1);
            var left = topology.ownerOf(LEFT).orElseThrow();
            var right = topology.ownerOf(RIGHT).orElseThrow();
            var delayed = topology.submitAfter(RIGHT, Long.MAX_VALUE, () -> 42);

            assertThatThrownBy(() -> topology.tryActivate(BRIDGE)).isInstanceOf(ArithmeticException.class);

            assertThat(topology.ownerOf(LEFT)).containsSame(left);
            assertThat(topology.ownerOf(RIGHT)).containsSame(right);
            assertThat(topology.activeCells()).containsExactlyInAnyOrder(LEFT, RIGHT);
            assertThat(delayed).isNotDone();
            assertThat(left.state()).isEqualTo(TickRegion.State.READY);
            assertThat(right.state()).isEqualTo(TickRegion.State.READY);
        }
    }

    private static void tick(TickRegionTopology topology, OwnershipCell cell, int count) {
        for (int index = 0; index < count; index++) {
            try (var context = topology.tryEnter(topology.ownerOf(cell).orElseThrow()).orElseThrow()) {
                context.runTick(ignored -> {});
            }
        }
    }
}
