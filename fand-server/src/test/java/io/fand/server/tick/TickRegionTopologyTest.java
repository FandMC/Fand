package io.fand.server.tick;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class TickRegionTopologyTest {

    private static final OwnershipCell LEFT = new OwnershipCell(0, 0);
    private static final OwnershipCell BRIDGE = new OwnershipCell(2, 0);
    private static final OwnershipCell RIGHT = new OwnershipCell(4, 0);

    @Test
    void chunkCoordinatesUseFloorDivisionIncludingNegativeBoundaries() {
        try (var topology = new TickRegionTopology(4, 1)) {
            assertThat(topology.cellAtChunk(-1, -4)).isEqualTo(new OwnershipCell(-1, -1));
            assertThat(topology.cellAtChunk(-5, 3)).isEqualTo(new OwnershipCell(-2, 0));
            assertThat(topology.cellAtChunk(4, 7)).isEqualTo(new OwnershipCell(1, 1));
            assertThat(topology.cellAtChunk(Integer.MIN_VALUE, Integer.MAX_VALUE))
                    .isEqualTo(new OwnershipCell(-536870912, 536870911));
        }
    }

    @Test
    void rejectsInvalidGridConfiguration() {
        assertThatThrownBy(() -> new TickRegionTopology(0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TickRegionTopology(3, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TickRegionTopology(4, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activeCellOwnsItsBufferWithoutActivatingTheBuffer() {
        try (var topology = new TickRegionTopology(4, 1)) {
            assertThat(topology.tryActivate(LEFT)).isEqualTo(TickRegionTopology.Change.APPLIED);
            var region = owner(topology, LEFT);

            assertThat(region.state()).isEqualTo(TickRegion.State.READY);
            assertThat(region.activeCells()).containsExactly(LEFT);
            assertThat(region.ownedCells()).hasSize(9).contains(new OwnershipCell(-1, -1));
            assertThat(topology.activeCells()).containsExactly(LEFT);
            assertThat(owner(topology, new OwnershipCell(1, 1))).isSameAs(region);
            assertThat(topology.ownerOf(BRIDGE)).isEmpty();
            assertThatThrownBy(() -> region.ownedCells().clear()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    void bufferOverlapMergesRegionsAndRetiresBothOldIdentities() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            topology.tryActivate(RIGHT);
            var left = owner(topology, LEFT);
            var right = owner(topology, RIGHT);
            assertThat(topology.regions()).hasSize(2);

            topology.tryActivate(BRIDGE);

            var merged = owner(topology, LEFT);
            assertThat(merged).isSameAs(owner(topology, RIGHT)).isSameAs(owner(topology, BRIDGE));
            assertThat(merged.activeCells()).containsExactlyInAnyOrder(LEFT, BRIDGE, RIGHT);
            assertThat(left.state()).isEqualTo(TickRegion.State.RETIRED);
            assertThat(right.state()).isEqualTo(TickRegion.State.RETIRED);
            assertThat(topology.tryEnter(left)).isEmpty();
            assertThat(topology.tryEnter(right)).isEmpty();
        }
    }

    @Test
    void removingBridgeSplitsOnlyTheAffectedRegion() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            topology.tryActivate(BRIDGE);
            topology.tryActivate(RIGHT);
            var distant = new OwnershipCell(20, -20);
            topology.tryActivate(distant);
            var unaffected = owner(topology, distant);
            var merged = owner(topology, LEFT);

            topology.tryDeactivate(BRIDGE);

            assertThat(topology.regions()).hasSize(3);
            assertThat(owner(topology, LEFT)).isNotSameAs(owner(topology, RIGHT));
            assertThat(owner(topology, distant)).isSameAs(unaffected);
            assertThat(merged.state()).isEqualTo(TickRegion.State.RETIRED);
            assertThat(topology.ownerOf(BRIDGE)).isEmpty();
        }
    }

    @Test
    void diagonalBufferContactAlsoRequiresOneRegion() {
        try (var topology = new TickRegionTopology(4, 1)) {
            var diagonal = new OwnershipCell(2, 2);
            topology.tryActivate(LEFT);
            topology.tryActivate(diagonal);

            assertThat(owner(topology, LEFT)).isSameAs(owner(topology, diagonal));
        }
    }

    @Test
    void duplicateActivationAndInactiveRemovalPreserveIdentity() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var original = owner(topology, LEFT);

            assertThat(topology.tryActivate(LEFT)).isEqualTo(TickRegionTopology.Change.UNCHANGED);
            assertThat(topology.tryDeactivate(new OwnershipCell(1, 1))).isEqualTo(TickRegionTopology.Change.UNCHANGED);
            assertThat(owner(topology, LEFT)).isSameAs(original);
        }
    }

    @Test
    void removingLastCellReleasesItsWholeBuffer() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var previous = owner(topology, LEFT);

            topology.tryDeactivate(LEFT);

            assertThat(topology.regions()).isEmpty();
            assertThat(topology.activeCells()).isEmpty();
            for (var cell : previous.ownedCells()) {
                assertThat(topology.ownerOf(cell)).isEmpty();
            }
            assertThat(previous.state()).isEqualTo(TickRegion.State.RETIRED);
        }
    }

    @Test
    void overflowingBufferCannotPartiallyPublishOwnership() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var previous = topology.regions();

            assertThatThrownBy(() -> topology.tryActivate(new OwnershipCell(Integer.MAX_VALUE, 0)))
                    .isInstanceOf(ArithmeticException.class);

            assertThat(topology.regions()).containsExactlyElementsOf(previous);
            assertThat(topology.activeCells()).containsExactly(LEFT);
            assertThat(previous.getFirst().state()).isEqualTo(TickRegion.State.READY);
        }
    }

    @Test
    void runningRegionRejectsMergeAndRemovalWithoutChangingEitherRegion() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            topology.tryActivate(RIGHT);
            var before = topology.regions();

            try (var context = topology.tryEnter(owner(topology, LEFT)).orElseThrow()) {
                assertThat(topology.tryActivate(BRIDGE)).isEqualTo(TickRegionTopology.Change.BUSY);
                assertThat(topology.tryDeactivate(LEFT)).isEqualTo(TickRegionTopology.Change.BUSY);
                assertThat(topology.regions()).containsExactlyElementsOf(before);
                assertThat(topology.activeCells()).containsExactlyInAnyOrder(LEFT, RIGHT);
                assertThat(context.owns(new OwnershipCell(1, 0))).isTrue();
                assertThat(context.owns(BRIDGE)).isFalse();
            }

            assertThat(topology.tryActivate(BRIDGE)).isEqualTo(TickRegionTopology.Change.APPLIED);
            assertThat(topology.regions()).hasSize(1);
        }
    }

    @Test
    void distantLayoutChangesDoNotDisturbAnActiveLease() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var left = owner(topology, LEFT);
            try (var context = topology.tryEnter(left).orElseThrow()) {
                assertThat(topology.tryActivate(RIGHT)).isEqualTo(TickRegionTopology.Change.APPLIED);
                assertThat(topology.tryDeactivate(RIGHT)).isEqualTo(TickRegionTopology.Change.APPLIED);
                assertThat(context.region()).isSameAs(left);
                assertThat(left.state()).isEqualTo(TickRegion.State.RUNNING);
                assertThat(context.owns(RIGHT)).isFalse();
            }
        }
    }

    @Test
    void equalRegionIdsFromDifferentWorldsAreNotInterchangeable() {
        try (var first = new TickRegionTopology(4, 1); var second = new TickRegionTopology(4, 1)) {
            first.tryActivate(LEFT);
            second.tryActivate(LEFT);
            var foreign = owner(second, LEFT);
            assertThat(foreign.id()).isEqualTo(owner(first, LEFT).id());

            assertThatThrownBy(() -> first.tryEnter(foreign)).isInstanceOf(IllegalArgumentException.class);
            assertThat(foreign.state()).isEqualTo(TickRegion.State.READY);
        }
    }

    @Test
    void randomActivationAndRemovalMatchIndependentConnectivityOracle() {
        for (int radius : List.of(1, 2)) {
            try (var topology = new TickRegionTopology(4, radius)) {
                var random = new Random(71031L + radius);
                var active = new HashSet<OwnershipCell>();
                for (int step = 0; step < 600; step++) {
                    var cell = new OwnershipCell(random.nextInt(21) - 10, random.nextInt(21) - 10);
                    boolean changed;
                    TickRegionTopology.Change result;
                    if (random.nextBoolean() && active.size() < 40 || active.isEmpty()) {
                        changed = active.add(cell);
                        result = topology.tryActivate(cell);
                    } else {
                        if (active.size() >= 40) {
                            cell = active.stream().sorted().skip(random.nextInt(active.size())).findFirst().orElseThrow();
                        }
                        changed = active.remove(cell);
                        result = topology.tryDeactivate(cell);
                    }
                    assertThat(result).isEqualTo(changed
                            ? TickRegionTopology.Change.APPLIED : TickRegionTopology.Change.UNCHANGED);
                    assertLayout(topology, active, radius);
                }
            }
        }
    }

    private static void assertLayout(TickRegionTopology topology, Set<OwnershipCell> active, int radius) {
        assertThat(topology.activeCells()).isEqualTo(active);
        var components = topology.regions().stream().map(TickRegion::activeCells).collect(Collectors.toSet());
        assertThat(components).isEqualTo(oracleComponents(active, radius));
        var claimed = new HashSet<OwnershipCell>();
        for (var region : topology.regions()) {
            var expectedBuffer = new HashSet<OwnershipCell>();
            for (var center : region.activeCells()) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        expectedBuffer.add(new OwnershipCell(center.x() + dx, center.z() + dz));
                    }
                }
            }
            assertThat(region.ownedCells()).isEqualTo(expectedBuffer);
            for (var cell : region.ownedCells()) {
                assertThat(claimed.add(cell)).as("unique ownership of %s", cell).isTrue();
                assertThat(owner(topology, cell)).isSameAs(region);
            }
        }
    }

    private static Set<Set<OwnershipCell>> oracleComponents(Set<OwnershipCell> active, int radius) {
        var remaining = new HashSet<>(active);
        var components = new HashSet<Set<OwnershipCell>>();
        while (!remaining.isEmpty()) {
            var root = remaining.iterator().next();
            remaining.remove(root);
            var component = new HashSet<OwnershipCell>();
            var frontier = new ArrayDeque<OwnershipCell>();
            frontier.add(root);
            while (!frontier.isEmpty()) {
                var next = frontier.removeFirst();
                component.add(next);
                var neighbors = remaining.stream().filter(candidate ->
                        Math.abs(candidate.x() - next.x()) <= 2 * radius
                                && Math.abs(candidate.z() - next.z()) <= 2 * radius).toList();
                remaining.removeAll(neighbors);
                frontier.addAll(neighbors);
            }
            components.add(component);
        }
        return components;
    }

    private static TickRegion owner(TickRegionTopology topology, OwnershipCell cell) {
        return topology.ownerOf(cell).orElseThrow();
    }
}
