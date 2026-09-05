package io.fand.server.tick;

import java.util.Optional;
import java.util.Objects;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * A thread-bound execution lease for a model region. This grants no NMS access
 * until the world data lifecycle is attached to the same ownership model.
 */
public final class RegionContext implements AutoCloseable {

    private static final ThreadLocal<RegionContext> CURRENT = new ThreadLocal<>();
    final TickRegion region;
    private final Thread thread = Thread.currentThread();
    boolean closed;
    boolean tasksDrained;
    boolean draining;
    boolean ticking;
    boolean tickCompleted;
    @Nullable Throwable failure;

    RegionContext(TickRegion region) {
        this.region = region;
    }

    public static Optional<RegionContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    static void requireUnbound() {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("A thread cannot enter another region while holding an execution lease");
        }
    }

    void bind() {
        CURRENT.set(this);
    }

    void unbind() {
        closed = true;
        CURRENT.remove();
    }

    public TickRegion region() {
        requireCurrent();
        return region;
    }

    public boolean owns(OwnershipCell cell) {
        requireCurrent();
        return region.ownedCells().contains(cell);
    }

    public void requireOwned(OwnershipCell cell) {
        if (!owns(cell)) {
            throw new IllegalStateException("Cell is not owned by the current region: " + cell);
        }
    }

    /** Drains tasks accepted before this call, once per execution lease. */
    public int runTasks() {
        requireCurrent();
        return region.topology.runTasks(this);
    }

    /** The local simulation tick being attempted, independent of server control ticks. */
    public long tickNumber() {
        requireCurrent();
        return Math.incrementExact(region.completedTicks());
    }

    /**
     * Runs one mailbox batch followed by the owned simulation. A simulation failure
     * retires execution permission for the whole region; successful completion
     * advances its local clock only when this lease is released.
     */
    public void runTick(Consumer<RegionContext> simulation) {
        Objects.requireNonNull(simulation, "simulation");
        requireCurrent();
        if (tasksDrained || ticking) {
            throw new IllegalStateException("Region execution has already started");
        }
        ticking = true;
        try {
            tickNumber();
            runTasks();
            simulation.accept(this);
            tickCompleted = true;
        } catch (RuntimeException | Error thrown) {
            failure = thrown;
            throw thrown;
        } finally {
            ticking = false;
        }
    }

    /** Runs a joined sub-phase without consuming a mailbox or advancing the full simulation clock. */
    public void runPhase(Consumer<RegionContext> simulation) {
        Objects.requireNonNull(simulation, "simulation");
        requireCurrent();
        if (tasksDrained || ticking) {
            throw new IllegalStateException("Region execution has already started");
        }
        ticking = true;
        try {
            simulation.accept(this);
        } catch (RuntimeException | Error thrown) {
            failure = thrown;
            throw thrown;
        } finally {
            ticking = false;
        }
    }

    void requireAttached() {
        if (Thread.currentThread() != thread || closed || CURRENT.get() != this) {
            throw new IllegalStateException("Region context is not active on this thread");
        }
    }

    void requireCurrent() {
        requireAttached();
        if (failure != null) {
            throw new IllegalStateException("Region execution has failed", failure);
        }
    }

    @Override
    public void close() {
        if (Thread.currentThread() != thread) {
            throw new IllegalStateException("Region context must be released by its execution thread");
        }
        if (!closed) {
            region.topology.release(this);
        }
    }
}
