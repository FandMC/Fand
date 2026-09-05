package io.fand.server.tick;

import java.util.Set;

/** An immutable ownership layout with an identity scoped to one loaded world's topology. */
public final class TickRegion {

    final TickRegionTopology topology;
    private final long id;
    private final Set<OwnershipCell> activeCells;
    private final Set<OwnershipCell> ownedCells;
    volatile State state = State.READY;
    volatile long completedTicks;

    TickRegion(TickRegionTopology topology, long id, Set<OwnershipCell> activeCells, Set<OwnershipCell> ownedCells,
               long completedTicks) {
        this.topology = topology;
        this.id = id;
        this.activeCells = Set.copyOf(activeCells);
        this.ownedCells = Set.copyOf(ownedCells);
        this.completedTicks = completedTicks;
    }

    public long id() {
        return id;
    }

    public State state() {
        return state;
    }

    public long completedTicks() {
        return completedTicks;
    }

    public Set<OwnershipCell> activeCells() {
        return activeCells;
    }

    public Set<OwnershipCell> ownedCells() {
        return ownedCells;
    }

    public enum State {
        READY,
        RUNNING,
        FAILED,
        RETIRED
    }
}
