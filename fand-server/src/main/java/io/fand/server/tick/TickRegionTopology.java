package io.fand.server.tick;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Per-world ownership and cell mailbox model. Overlapping cell buffers form a
 * single region. Layout changes affect only idle regions and publish under one
 * metadata lock; actions and future completion callbacks run outside that lock.
 *
 * <p>This is not a chunk loader or a tick executor. Callers supply active-cell
 * transitions and must retry BUSY changes at a safe point before publishing new
 * world data. Buffer size still needs validation against actual NMS access paths.
 */
public final class TickRegionTopology implements AutoCloseable {

    private final Object lock = new Object();
    private final int cellSizeChunks;
    private final int bufferRadiusCells;
    private final Map<OwnershipCell, CellEntry> activeCells = new HashMap<>();
    private final Map<OwnershipCell, TickRegion> owners = new HashMap<>();
    private long nextRegionId;
    private long nextTaskSequence;
    private boolean closed;
    private boolean stopping;
    private @Nullable Thread maintenanceThread;
    private final List<Runnable> maintenanceCompletions = new ArrayList<>();

    public TickRegionTopology(int cellSizeChunks, int bufferRadiusCells) {
        if (cellSizeChunks <= 0 || (cellSizeChunks & (cellSizeChunks - 1)) != 0) {
            throw new IllegalArgumentException("cellSizeChunks must be a positive power of two");
        }
        if (bufferRadiusCells < 1) {
            throw new IllegalArgumentException("bufferRadiusCells must be positive");
        }
        this.cellSizeChunks = cellSizeChunks;
        this.bufferRadiusCells = bufferRadiusCells;
    }

    public OwnershipCell cellAtChunk(int chunkX, int chunkZ) {
        return new OwnershipCell(Math.floorDiv(chunkX, cellSizeChunks), Math.floorDiv(chunkZ, cellSizeChunks));
    }

    public Optional<TickRegion> ownerOf(OwnershipCell cell) {
        Objects.requireNonNull(cell, "cell");
        synchronized (lock) {
            return Optional.ofNullable(owners.get(cell));
        }
    }

    /** Returns a snapshot of region membership; each handle's execution state remains observable. */
    public List<TickRegion> regions() {
        synchronized (lock) {
            return owners.values().stream().distinct().sorted(Comparator.comparingLong(TickRegion::id)).toList();
        }
    }

    public Set<OwnershipCell> activeCells() {
        synchronized (lock) {
            return Set.copyOf(activeCells.keySet());
        }
    }

    public Change tryActivate(OwnershipCell cell) {
        Objects.requireNonNull(cell, "cell");
        synchronized (lock) {
            requireOpen();
            if (activeCells.containsKey(cell)) {
                return Change.UNCHANGED;
            }
            var buffer = bufferOf(cell);
            var affected = new HashSet<TickRegion>();
            for (var position : buffer) {
                var region = owners.get(position);
                if (region != null) {
                    affected.add(region);
                }
            }
            if (hasRunningRegion(affected)) {
                return Change.BUSY;
            }
            var mergedCells = new HashSet<OwnershipCell>();
            mergedCells.add(cell);
            affected.forEach(region -> mergedCells.addAll(region.activeCells()));
            long completedTicks = affected.stream().mapToLong(TickRegion::completedTicks).max().orElse(0);
            var replacement = newRegion(mergedCells, completedTicks);
            replace(affected, List.of(replacement));
            activeCells.put(cell, new CellEntry());
            return Change.APPLIED;
        }
    }

    public Change tryDeactivate(OwnershipCell cell) {
        Objects.requireNonNull(cell, "cell");
        List<PendingCall<?>> cancelled;
        synchronized (lock) {
            requireOpen();
            var entry = activeCells.get(cell);
            if (entry == null) {
                return Change.UNCHANGED;
            }
            var region = owners.get(cell);
            if (hasRunningRegion(Set.of(region))) {
                return Change.BUSY;
            }
            var remaining = new HashSet<>(region.activeCells());
            remaining.remove(cell);
            var replacements = connectedComponents(remaining).stream()
                    .map(cells -> newRegion(cells, region.completedTicks())).toList();
            replace(Set.of(region), replacements);
            activeCells.remove(cell);
            cancelled = drain(entry);
        }
        completeAfterMaintenance(() -> cancelled.forEach(call -> call.result.cancel(false)));
        return Change.APPLIED;
    }

    public Optional<RegionContext> tryEnter(TickRegion region) {
        Objects.requireNonNull(region, "region");
        RegionContext.requireUnbound();
        synchronized (lock) {
            if (region.topology != this) {
                throw new IllegalArgumentException("Region belongs to another world topology");
            }
            if (closed || stopping || maintenanceThread != null || region.state != TickRegion.State.READY) {
                return Optional.empty();
            }
            var context = new RegionContext(region);
            region.state = TickRegion.State.RUNNING;
            context.bind();
            return Optional.of(context);
        }
    }

    public <T> CompletableFuture<T> submit(OwnershipCell cell, Supplier<T> action) {
        return submit(cell, 0, action);
    }

    /**
     * Schedules against completed local simulation ticks, not wall time. A delay
     * submitted during a running tick starts after that tick. Layout replacement
     * preserves remaining ticks even when regions with different clocks merge.
     */
    public <T> CompletableFuture<T> submitAfter(OwnershipCell cell, long delayTicks, Supplier<T> action) {
        if (delayTicks <= 0) {
            throw new IllegalArgumentException("Tick delay must be positive");
        }
        return submit(cell, delayTicks, action);
    }

    private <T> CompletableFuture<T> submit(OwnershipCell cell, long delayTicks, Supplier<T> action) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(action, "action");
        CellEntry entry;
        PendingCall<T> call;
        synchronized (lock) {
            entry = activeCells.get(cell);
            if (closed || stopping || entry == null) {
                return CompletableFuture.failedFuture(new RejectedExecutionException("Ownership cell is not active"));
            }
            var region = owners.get(cell);
            if (region.state == TickRegion.State.FAILED) {
                return CompletableFuture.failedFuture(new RejectedExecutionException("Region execution has failed"));
            }
            long sequence = nextTaskSequence;
            nextTaskSequence = Math.incrementExact(nextTaskSequence);
            long baseTick = region.completedTicks();
            if (delayTicks > 0 && region.state == TickRegion.State.RUNNING) {
                baseTick = Math.incrementExact(baseTick);
            }
            long dueTick = Math.addExact(baseTick, Math.max(1, delayTicks));
            call = new PendingCall<>(sequence, dueTick, action);
            entry.pending.add(call);
        }
        call.result.whenComplete((value, failure) -> {
            synchronized (lock) {
                entry.pending.remove(call);
            }
        });
        return call.result;
    }

    int runTasks(RegionContext context) {
        var ready = new ArrayList<PendingCall<?>>();
        synchronized (lock) {
            context.requireCurrent();
            long tickNumber = context.tickNumber();
            if (context.tasksDrained) {
                throw new IllegalStateException("Region tasks have already been drained for this execution");
            }
            context.tasksDrained = true;
            context.draining = true;
            for (var cell : context.region.activeCells()) {
                var pending = activeCells.get(cell).pending;
                while (!pending.isEmpty() && pending.first().dueTick <= tickNumber) {
                    ready.add(pending.pollFirst());
                }
            }
        }
        ready.sort(Comparator.comparingLong(call -> call.sequence));
        int executed = 0;
        try {
            for (var call : ready) {
                if (!call.result.isDone()) {
                    executed++;
                    call.execute();
                }
            }
        } catch (Error failure) {
            context.failure = failure;
            ready.forEach(call -> call.result.completeExceptionally(failure));
            throw failure;
        } finally {
            context.draining = false;
        }
        return executed;
    }

    void release(RegionContext context) {
        var failedCalls = new ArrayList<PendingCall<?>>();
        synchronized (lock) {
            context.requireAttached();
            if (context.draining || context.ticking) {
                throw new IllegalStateException("Cannot release a region while its task batch is executing");
            }
            if (context.failure == null) {
                if (context.tickCompleted) {
                    context.region.completedTicks = Math.incrementExact(context.region.completedTicks);
                }
                context.region.state = TickRegion.State.READY;
            } else {
                context.region.state = TickRegion.State.FAILED;
                for (var cell : context.region.activeCells()) {
                    failedCalls.addAll(drain(activeCells.get(cell)));
                }
            }
            context.unbind();
        }
        failedCalls.forEach(call -> call.result.completeExceptionally(context.failure));
    }

    @Override
    public void close() {
        var cancelled = new ArrayList<PendingCall<?>>();
        synchronized (lock) {
            requireMaintenanceThread();
            if (closed) {
                return;
            }
            var regions = new HashSet<>(owners.values());
            if (regions.stream().anyMatch(region -> region.state == TickRegion.State.RUNNING)) {
                throw new IllegalStateException("Cannot close a topology with active execution leases");
            }
            closed = true;
            regions.forEach(region -> region.state = TickRegion.State.RETIRED);
            activeCells.values().forEach(entry -> cancelled.addAll(drain(entry)));
            activeCells.clear();
            owners.clear();
        }
        completeAfterMaintenance(() -> cancelled.forEach(call -> call.result.cancel(false)));
    }

    /** Stops new execution and submissions without interrupting existing leases. */
    void stopAdmission() {
        synchronized (lock) {
            stopping = true;
        }
    }

    /**
     * Reserves admission for a short control-plane pass without waiting for active
     * regions. Changes to running regions still return BUSY. Completion callbacks
     * are deferred until admission is released; no callback runs under the lock.
     */
    void maintain(Runnable action) {
        RegionContext.requireUnbound();
        synchronized (lock) {
            if (closed || maintenanceThread != null) {
                throw new IllegalStateException("Topology maintenance is unavailable");
            }
            maintenanceThread = Thread.currentThread();
        }
        try {
            action.run();
        } finally {
            List<Runnable> completions;
            synchronized (lock) {
                maintenanceThread = null;
                completions = List.copyOf(maintenanceCompletions);
                maintenanceCompletions.clear();
            }
            completions.forEach(Runnable::run);
        }
    }

    private void completeAfterMaintenance(Runnable completion) {
        synchronized (lock) {
            if (maintenanceThread == Thread.currentThread()) {
                maintenanceCompletions.add(completion);
                return;
            }
        }
        completion.run();
    }

    private void requireMaintenanceThread() {
        if (maintenanceThread != null && maintenanceThread != Thread.currentThread()) {
            throw new IllegalStateException("Topology is being maintained by another thread");
        }
    }

    private TickRegion newRegion(Set<OwnershipCell> cells, long completedTicks) {
        var owned = new HashSet<OwnershipCell>();
        cells.forEach(cell -> owned.addAll(bufferOf(cell)));
        long id = nextRegionId;
        nextRegionId = Math.incrementExact(nextRegionId);
        return new TickRegion(this, id, cells, owned, completedTicks);
    }

    private void replace(Set<TickRegion> previous, List<TickRegion> replacements) {
        var plannedOwners = new HashSet<OwnershipCell>();
        var translatedDeadlines = new HashMap<PendingCall<?>, Long>();
        var translatedEntries = new HashMap<CellEntry, List<PendingCall<?>>>();
        for (var replacement : replacements) {
            for (var cell : replacement.ownedCells()) {
                var existing = owners.get(cell);
                if (!plannedOwners.add(cell) || existing != null && !previous.contains(existing)) {
                    throw new IllegalStateException("Replacement regions have conflicting ownership");
                }
            }
        }
        // Validate every deadline before changing any published owner or mailbox.
        for (var replacement : replacements) {
            for (var cell : replacement.activeCells()) {
                var entry = activeCells.get(cell);
                if (entry != null) {
                    long sourceTick = owners.get(cell).completedTicks();
                    long shift = Math.subtractExact(replacement.completedTicks(), sourceTick);
                    if (shift != 0 && !entry.pending.isEmpty()) {
                        translatedEntries.put(entry, List.copyOf(entry.pending));
                        for (var call : entry.pending) {
                            translatedDeadlines.put(call, Math.addExact(call.dueTick, shift));
                        }
                    }
                }
            }
        }
        translatedEntries.keySet().forEach(entry -> entry.pending.clear());
        translatedDeadlines.forEach((call, deadline) -> call.dueTick = deadline);
        translatedEntries.forEach((entry, calls) -> entry.pending.addAll(calls));
        for (var region : previous) {
            region.ownedCells().forEach(owners::remove);
            region.state = TickRegion.State.RETIRED;
        }
        for (var region : replacements) {
            region.ownedCells().forEach(cell -> owners.put(cell, region));
        }
    }

    private Set<OwnershipCell> bufferOf(OwnershipCell cell) {
        int minX = Math.subtractExact(cell.x(), bufferRadiusCells);
        int maxX = Math.addExact(cell.x(), bufferRadiusCells);
        int minZ = Math.subtractExact(cell.z(), bufferRadiusCells);
        int maxZ = Math.addExact(cell.z(), bufferRadiusCells);
        var buffer = new HashSet<OwnershipCell>();
        for (long x = minX; x <= maxX; x++) {
            for (long z = minZ; z <= maxZ; z++) {
                buffer.add(new OwnershipCell((int) x, (int) z));
            }
        }
        return buffer;
    }

    private List<Set<OwnershipCell>> connectedComponents(Set<OwnershipCell> cells) {
        var remaining = new TreeSet<>(cells);
        var components = new ArrayList<Set<OwnershipCell>>();
        long reach = 2L * bufferRadiusCells;
        while (!remaining.isEmpty()) {
            var component = new HashSet<OwnershipCell>();
            var frontier = new ArrayDeque<OwnershipCell>();
            frontier.add(remaining.pollFirst());
            while (!frontier.isEmpty()) {
                var cell = frontier.removeFirst();
                component.add(cell);
                for (long x = (long) cell.x() - reach; x <= (long) cell.x() + reach; x++) {
                    for (long z = (long) cell.z() - reach; z <= (long) cell.z() + reach; z++) {
                        if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE
                                || z < Integer.MIN_VALUE || z > Integer.MAX_VALUE) {
                            continue;
                        }
                        var neighbor = new OwnershipCell((int) x, (int) z);
                        if (remaining.remove(neighbor)) {
                            frontier.addLast(neighbor);
                        }
                    }
                }
            }
            components.add(component);
        }
        return components;
    }

    private static boolean hasRunningRegion(Set<TickRegion> regions) {
        if (regions.stream().anyMatch(region -> region.state == TickRegion.State.FAILED)) {
            throw new IllegalStateException("Cannot change ownership of a failed region");
        }
        return regions.stream().anyMatch(region -> region.state == TickRegion.State.RUNNING);
    }

    private static List<PendingCall<?>> drain(CellEntry entry) {
        var calls = List.copyOf(entry.pending);
        entry.pending.clear();
        return calls;
    }

    private void requireOpen() {
        requireMaintenanceThread();
        if (closed) {
            throw new IllegalStateException("World topology is closed");
        }
    }

    public enum Change {
        APPLIED,
        UNCHANGED,
        BUSY
    }

    private static final class CellEntry {
        private final NavigableSet<PendingCall<?>> pending = new TreeSet<>(
                Comparator.<PendingCall<?>>comparingLong(call -> call.dueTick)
                        .thenComparingLong(call -> call.sequence));
    }

    private static final class PendingCall<T> {
        private final long sequence;
        private long dueTick;
        private final Supplier<T> action;
        private final CompletableFuture<T> result = new CompletableFuture<>();

        private PendingCall(long sequence, long dueTick, Supplier<T> action) {
            this.sequence = sequence;
            this.dueTick = dueTick;
            this.action = action;
        }

        private void execute() {
            try {
                result.complete(action.get());
            } catch (RuntimeException failure) {
                result.completeExceptionally(failure);
            }
        }
    }
}
