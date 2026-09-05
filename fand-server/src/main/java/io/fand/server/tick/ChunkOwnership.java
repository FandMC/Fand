package io.fand.server.tick;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Aggregates chunk-holder lifetimes into active cells for one loaded world.
 * Requests may arrive on any thread; {@link #drain} belongs to the creating
 * control thread. This object must be the sole writer of its topology.
 *
 * <p>Register before publishing a holder to live world state. A pending-unload
 * holder must remain registered until its final unload, including save waits.
 * Use the returned handle to publish asynchronous results under ownership.
 * This adapter does not itself install, save or unload Minecraft chunks.
 * Cancelling an observation future does not cancel a lifecycle operation.
 */
public final class ChunkOwnership<H> {

    private final Thread controlThread = Thread.currentThread();
    private final TickRegionTopology topology;
    private final Object inboxLock = new Object();
    private final ArrayDeque<Request<H>> inbox = new ArrayDeque<>();
    private final Map<Long, Handle<H>> installed = new HashMap<>();
    private final Map<Long, Handle<H>> desired = new HashMap<>();
    private final Map<OwnershipCell, Integer> chunkCounts = new HashMap<>();
    private final Set<Long> pending = new LinkedHashSet<>();
    private final CompletableFuture<Void> terminated = new CompletableFuture<>();
    private volatile boolean stopping;
    private boolean closed;
    private boolean draining;

    public ChunkOwnership(int cellSizeChunks, int bufferRadiusCells) {
        topology = new TickRegionTopology(cellSizeChunks, bufferRadiusCells);
    }

    /** The execution side may inspect ownership and acquire leases, but must not change cells. */
    public TickRegionTopology topology() {
        return topology;
    }

    /**
     * Creates a new lifetime, even if a previous registration used the same holder
     * object or coordinates. The latest accepted registration replaces the old one
     * at a safe point. Wait for {@link Handle#ready()} before using this lifetime.
     */
    public Handle<H> register(int chunkX, int chunkZ, H holder) {
        var handle = new Handle<>(this, chunkX, chunkZ, Objects.requireNonNull(holder, "holder"));
        boolean accepted;
        synchronized (inboxLock) {
            accepted = !stopping;
            if (accepted) {
                inbox.addLast(new Request<>(handle, true));
            }
        }
        if (!accepted) {
            handle.reject(new RejectedExecutionException("World ownership is stopping"));
        }
        return handle;
    }

    /**
     * Stops admission immediately. The control thread must continue draining until
     * this future completes; an in-flight execution lease is allowed to finish.
     */
    public CompletableFuture<Void> shutdown() {
        synchronized (inboxLock) {
            stopping = true;
            topology.stopAdmission();
        }
        return terminated.copy();
    }

    /**
     * Consumes at most maxRequests requests and attempts at most maxChanges layout
     * changes. Busy coordinates rotate to the back and never block unrelated cells.
     * No waiting, sleeping or retry loop occurs inside a pass.
     */
    public DrainResult drain(int maxRequests, int maxChanges) {
        requireControlThread();
        if (maxRequests <= 0 || maxChanges <= 0) {
            throw new IllegalArgumentException("Drain budgets must be positive");
        }
        if (draining) {
            throw new IllegalStateException("Ownership maintenance cannot be reentered");
        }
        if (closed) {
            return new DrainResult(0, 0, 0, 0, 0, true);
        }
        var notifications = new ArrayList<Runnable>();
        int[] counts = new int[3];
        draining = true;
        try {
            topology.maintain(() -> {
                // Poll individually so a failed request cannot strand a detached inbox batch.
                while (counts[0] < maxRequests) {
                    Request<H> request;
                    synchronized (inboxLock) {
                        request = inbox.pollFirst();
                    }
                    if (request == null) {
                        break;
                    }
                    counts[0]++;
                    accept(request, notifications);
                }
                if (stopping) {
                    finishShutdown(notifications);
                    return;
                }
                int attempts = Math.min(maxChanges, pending.size());
                for (int index = 0; index < attempts; index++) {
                    var iterator = pending.iterator();
                    long position = iterator.next();
                    iterator.remove();
                    try {
                        if (apply(position, notifications)) {
                            counts[1]++;
                        } else {
                            counts[2]++;
                            pending.add(position);
                        }
                    } catch (RuntimeException | Error failure) {
                        var target = desired.get(position);
                        if (target != null && target != installed.get(position)) {
                            desired.remove(position);
                            target.rejectLater(failure, notifications);
                        }
                        if (installed.containsKey(position)) {
                            pending.add(position);
                        }
                        throw failure;
                    }
                }
            });
        } finally {
            // State is fully published and admission is restored before observers run.
            try {
                notifications.forEach(Runnable::run);
            } finally {
                draining = false;
            }
        }
        synchronized (inboxLock) {
            return new DrainResult(counts[0], counts[1], counts[2], inbox.size(), pending.size(), closed);
        }
    }

    public int installedChunks() {
        requireControlThread();
        return installed.size();
    }

    public int chunksIn(OwnershipCell cell) {
        requireControlThread();
        return chunkCounts.getOrDefault(Objects.requireNonNull(cell, "cell"), 0);
    }

    private void retire(Handle<H> handle) {
        if (handle.retirementRequested.compareAndSet(false, true)) {
            synchronized (inboxLock) {
                if (!stopping) {
                    inbox.addLast(new Request<>(handle, false));
                }
            }
        }
    }

    private void accept(Request<H> request, List<Runnable> notifications) {
        var handle = request.handle;
        long position = handle.position;
        if (request.register) {
            if (stopping || handle.retirementRequested.get()) {
                handle.retireLater(notifications);
                return;
            }
            var previous = desired.put(position, handle);
            if (previous != null && previous != installed.get(position)) {
                previous.retireLater(notifications);
            }
            pending.add(position);
        } else if (desired.get(position) == handle) {
            desired.remove(position);
            if (installed.get(position) != handle) {
                handle.retireLater(notifications);
            }
            pending.add(position);
        }
    }

    private boolean apply(long position, List<Runnable> notifications) {
        var current = installed.get(position);
        var target = desired.get(position);
        if (target != null && target.retirementRequested.get()) {
            desired.remove(position);
            if (target != current) {
                target.retireLater(notifications);
            }
            target = null;
        }
        if (current == target) {
            return true;
        }
        var cell = (current != null ? current : Objects.requireNonNull(target)).cell;
        int count = chunkCounts.getOrDefault(cell, 0);
        var owner = topology.ownerOf(cell).orElse(null);
        if (owner != null) {
            if (owner.state() == TickRegion.State.RUNNING) {
                return false;
            }
            if (owner.state() == TickRegion.State.FAILED) {
                throw new IllegalStateException("Cannot publish holders into a failed region");
            }
        }
        if (current == null && count == 0) {
            if (topology.tryActivate(cell) == TickRegionTopology.Change.BUSY) {
                return false;
            }
        } else if (target == null && count == 1) {
            if (topology.tryDeactivate(cell) == TickRegionTopology.Change.BUSY) {
                return false;
            }
        }
        if (current != null) {
            installed.remove(position);
            count--;
            current.retireLater(notifications);
        }
        if (target != null) {
            installed.put(position, target);
            count++;
            target.state = Handle.State.ACTIVE;
            var activated = target;
            notifications.add(() -> activated.ready.complete(null));
        }
        if (count == 0) {
            chunkCounts.remove(cell);
        } else {
            chunkCounts.put(cell, count);
        }
        return true;
    }

    private void finishShutdown(List<Runnable> notifications) {
        synchronized (inboxLock) {
            if (!inbox.isEmpty()) {
                return;
            }
        }
        if (topology.regions().stream().anyMatch(region -> region.state() == TickRegion.State.RUNNING)) {
            return;
        }
        var handles = new LinkedHashSet<>(installed.values());
        handles.addAll(desired.values());
        handles.forEach(handle -> handle.retireLater(notifications));
        installed.clear();
        desired.clear();
        chunkCounts.clear();
        pending.clear();
        closed = true;
        topology.close();
        notifications.add(() -> terminated.complete(null));
    }

    private void requireControlThread() {
        if (Thread.currentThread() != controlThread) {
            throw new IllegalStateException("Ownership maintenance belongs to the world control thread");
        }
    }

    public record DrainResult(int requests, int applied, int busy, int queuedRequests, int pendingChanges,
                              boolean closed) {}

    private record Request<H>(Handle<H> handle, boolean register) {}

    /** An identity token for one chunk-holder lifetime, never revived after retirement. */
    public static final class Handle<H> {
        private final ChunkOwnership<H> ownership;
        private final long position;
        private final OwnershipCell cell;
        private final H holder;
        private final AtomicBoolean retirementRequested = new AtomicBoolean();
        private final Set<CompletableFuture<?>> tasks = ConcurrentHashMap.newKeySet();
        private final CompletableFuture<Void> ready = new CompletableFuture<>();
        private final CompletableFuture<Void> retired = new CompletableFuture<>();
        private volatile State state = State.PENDING;

        private Handle(ChunkOwnership<H> ownership, int chunkX, int chunkZ, H holder) {
            this.ownership = ownership;
            position = ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
            cell = ownership.topology.cellAtChunk(chunkX, chunkZ);
            this.holder = holder;
        }

        public OwnershipCell cell() {
            return cell;
        }

        public State state() {
            return state;
        }

        public CompletableFuture<Void> ready() {
            return ready.copy();
        }

        /** Observes retirement without requesting it. */
        public CompletableFuture<Void> whenRetired() {
            return retired.copy();
        }

        /** Requests retirement; repeated calls observe the same lifecycle transition. */
        public CompletableFuture<Void> retire() {
            ownership.retire(this);
            return retired.copy();
        }

        /**
         * Routes an asynchronous result to the current owner of this exact holder
         * lifetime. The identity is checked again at execution, including when the
         * containing cell remains active due to other chunks or a replacement.
         */
        public <T> CompletableFuture<T> submit(Function<? super H, ? extends T> action) {
            return submit(0, action);
        }

        /** Delays a lifetime-bound action by completed local simulation ticks. */
        public <T> CompletableFuture<T> submitAfter(long delayTicks, Function<? super H, ? extends T> action) {
            if (delayTicks <= 0) {
                throw new IllegalArgumentException("Tick delay must be positive");
            }
            return submit(delayTicks, action);
        }

        private <T> CompletableFuture<T> submit(long delayTicks, Function<? super H, ? extends T> action) {
            Objects.requireNonNull(action, "action");
            if (!acceptsTasks()) {
                return CompletableFuture.failedFuture(new RejectedExecutionException("Chunk holder is not active"));
            }
            Supplier<T> ownedAction = () -> {
                if (!acceptsTasks()) {
                    throw new CancellationException("Chunk-holder lifetime has ended");
                }
                return action.apply(holder);
            };
            CompletableFuture<T> result = delayTicks == 0
                    ? ownership.topology.submit(cell, ownedAction)
                    : ownership.topology.submitAfter(cell, delayTicks, ownedAction);
            tasks.add(result);
            result.whenComplete((value, failure) -> tasks.remove(result));
            // Retirement may have drained the set between the initial check and insertion.
            if (!acceptsTasks()) {
                result.cancel(false);
            }
            return result;
        }

        private boolean acceptsTasks() {
            return state == State.ACTIVE && !retirementRequested.get();
        }

        private void retireLater(List<Runnable> notifications) {
            if (state == State.RETIRED || state == State.REJECTED) {
                return;
            }
            state = State.RETIRED;
            notifications.add(() -> {
                tasks.forEach(task -> task.cancel(false));
                ready.cancel(false);
                retired.complete(null);
            });
        }

        private void rejectLater(Throwable failure, List<Runnable> notifications) {
            state = State.REJECTED;
            notifications.add(() -> reject(failure));
        }

        private void reject(Throwable failure) {
            state = State.REJECTED;
            ready.completeExceptionally(failure);
            retired.complete(null);
        }

        public enum State {
            PENDING,
            ACTIVE,
            RETIRED,
            REJECTED
        }
    }
}
