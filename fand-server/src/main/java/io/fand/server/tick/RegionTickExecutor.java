package io.fand.server.tick;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * Dispatches independent model ticks on a caller-owned executor. At most one tick
 * per region identity is queued or running here. There is no all-region barrier,
 * catch-up loop or built-in 20 TPS pacing; the control plane decides when to submit
 * the next tick and retry UNAVAILABLE results after maintenance or retirement.
 *
 * <p>This class must not execute NMS ticks until their state is region-owned.
 */
public final class RegionTickExecutor {

    private final Object lock = new Object();
    private final Executor executor;
    private final Consumer<RegionContext> simulation;
    private final boolean completesTick;
    private final Map<TickRegion, Dispatch> pending = new IdentityHashMap<>();
    private final CompletableFuture<Void> terminated = new CompletableFuture<>();
    private boolean stopping;

    public RegionTickExecutor(Executor executor, Consumer<RegionContext> simulation) {
        this(executor, simulation, true);
    }

    private RegionTickExecutor(Executor executor, Consumer<RegionContext> simulation, boolean completesTick) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.simulation = Objects.requireNonNull(simulation, "simulation");
        this.completesTick = completesTick;
    }

    public static RegionTickExecutor forPhases(Executor executor, Consumer<RegionContext> simulation) {
        return new RegionTickExecutor(executor, simulation, false);
    }

    /** Cancelling the returned observation never interrupts a live simulation. */
    public CompletableFuture<Result> tick(TickRegion region) {
        Objects.requireNonNull(region, "region");
        Dispatch dispatch;
        synchronized (lock) {
            if (stopping) {
                return CompletableFuture.failedFuture(new RejectedExecutionException("Region tick executor is stopping"));
            }
            var existing = pending.get(region);
            if (existing != null) {
                return existing.result.copy();
            }
            dispatch = new Dispatch(region);
            pending.put(region, dispatch);
        }
        try {
            executor.execute(() -> execute(dispatch));
        } catch (RuntimeException failure) {
            finish(dispatch, null, failure);
        } catch (Error failure) {
            finish(dispatch, null, failure);
            throw failure;
        }
        return dispatch.result.copy();
    }

    /** Cancels queued ticks and completes after all running ticks release their leases. */
    public CompletableFuture<Void> shutdown() {
        var cancelled = new ArrayList<Dispatch>();
        synchronized (lock) {
            stopping = true;
            var iterator = pending.values().iterator();
            while (iterator.hasNext()) {
                var dispatch = iterator.next();
                if (!dispatch.started) {
                    cancelled.add(dispatch);
                    iterator.remove();
                }
            }
        }
        cancelled.forEach(dispatch -> dispatch.result.cancel(false));
        completeShutdown();
        return terminated.copy();
    }

    private void execute(Dispatch dispatch) {
        synchronized (lock) {
            if (pending.get(dispatch.region) != dispatch || dispatch.started) {
                return;
            }
            dispatch.started = true;
        }
        Result result = Result.UNAVAILABLE;
        Throwable failure = null;
        try {
            var lease = dispatch.region.topology.tryEnter(dispatch.region);
            if (lease.isPresent()) {
                try (var context = lease.orElseThrow()) {
                    if (completesTick) {
                        context.runTick(simulation);
                    } else {
                        context.runPhase(simulation);
                    }
                }
                result = Result.EXECUTED;
            }
        } catch (RuntimeException | Error thrown) {
            failure = thrown;
        }
        finish(dispatch, result, failure);
        if (failure instanceof Error error) {
            throw error;
        }
    }

    private void finish(Dispatch dispatch, @Nullable Result result, @Nullable Throwable failure) {
        synchronized (lock) {
            pending.remove(dispatch.region, dispatch);
        }
        if (failure != null) {
            dispatch.result.completeExceptionally(failure);
        } else {
            dispatch.result.complete(Objects.requireNonNull(result));
        }
        completeShutdown();
    }

    private void completeShutdown() {
        boolean complete;
        synchronized (lock) {
            complete = stopping && pending.isEmpty();
        }
        if (complete) {
            terminated.complete(null);
        }
    }

    public enum Result {
        EXECUTED,
        UNAVAILABLE
    }

    private static final class Dispatch {
        private final TickRegion region;
        private final CompletableFuture<Result> result = new CompletableFuture<>();
        private boolean started;

        private Dispatch(TickRegion region) {
            this.region = region;
        }
    }
}
