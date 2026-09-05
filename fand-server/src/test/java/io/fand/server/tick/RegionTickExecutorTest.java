package io.fand.server.tick;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegionTickExecutorTest {
    private static final OwnershipCell LEFT = new OwnershipCell(0, 0);
    private static final OwnershipCell RIGHT = new OwnershipCell(4, 0);
    private static final OwnershipCell BRIDGE = new OwnershipCell(2, 0);

    @Test
    void independentRegionTicksAndHolderPublicationDoNotWaitForASlowRegion() throws Exception {
        var ownership = new ChunkOwnership<AtomicInteger>(1, 1);
        var left = ownership.register(0, 0, new AtomicInteger());
        var right = ownership.register(4, 0, new AtomicInteger());
        ownership.drain(20, 20);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var executor = new RegionTickExecutor(workers, context -> {
                if (context.owns(LEFT)) {
                    entered.countDown();
                    await(release);
                }
            });
            var leftTick = executor.tick(ownership.topology().ownerOf(LEFT).orElseThrow());
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                var published = right.submit(AtomicInteger::incrementAndGet);
                var rightRegion = ownership.topology().ownerOf(RIGHT).orElseThrow();
                assertThat(executor.tick(rightRegion).get(5, TimeUnit.SECONDS))
                        .isEqualTo(RegionTickExecutor.Result.EXECUTED);
                assertThat(published).isCompletedWithValue(1);
                assertThat(rightRegion.completedTicks()).isEqualTo(1);
                assertThat(leftTick).isNotDone();

                var bridge = ownership.register(2, 0, new AtomicInteger());
                assertThat(ownership.drain(20, 20).busy()).isEqualTo(1);
                assertThat(bridge.ready()).isNotDone();
            } finally {
                release.countDown();
            }
            assertThat(leftTick.get(5, TimeUnit.SECONDS)).isEqualTo(RegionTickExecutor.Result.EXECUTED);
            ownership.drain(20, 20);
            assertThat(ownership.topology().regions()).hasSize(1);
            assertThat(left.ready()).isCompleted();
            assertThat(executor.shutdown().get(5, TimeUnit.SECONDS)).isNull();
        }
        var stopped = ownership.shutdown();
        ownership.drain(20, 20);
        assertThat(stopped).isCompleted();
    }

    @Test
    void duplicateQueuedTicksShareOneExecutionAndCancellationOnlyAffectsTheObserver() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var worker = new QueuedExecutor();
            var calls = new AtomicInteger();
            var executor = new RegionTickExecutor(worker, ignored -> calls.incrementAndGet());
            var region = topology.ownerOf(LEFT).orElseThrow();
            var first = executor.tick(region);
            var second = executor.tick(region);
            first.cancel(false);

            assertThat(worker.queue).hasSize(1);
            worker.runNext();

            assertThat(first).isCancelled();
            assertThat(second).isCompletedWithValue(RegionTickExecutor.Result.EXECUTED);
            assertThat(calls).hasValue(1);
            assertThat(region.completedTicks()).isEqualTo(1);
            assertThat(RegionContext.current()).isEmpty();
            var next = executor.tick(region);
            worker.runNext();
            assertThat(next).isCompletedWithValue(RegionTickExecutor.Result.EXECUTED);
            assertThat(region.completedTicks()).isEqualTo(2);
        }
    }

    @Test
    void aQueuedTickForARetiredLayoutDoesNotExecuteOnTheReplacement() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var worker = new QueuedExecutor();
            var calls = new AtomicInteger();
            var executor = new RegionTickExecutor(worker, ignored -> calls.incrementAndGet());
            var old = topology.ownerOf(LEFT).orElseThrow();
            var result = executor.tick(old);
            topology.tryActivate(BRIDGE);
            worker.runNext();

            assertThat(result).isCompletedWithValue(RegionTickExecutor.Result.UNAVAILABLE);
            assertThat(calls).hasValue(0);
            assertThat(old.completedTicks()).isZero();
            assertThat(topology.ownerOf(LEFT).orElseThrow().completedTicks()).isZero();
        }
    }

    @Test
    void maintenanceDeniesAdmissionWithoutLosingTheMailbox() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var calls = new AtomicInteger();
            var executor = new RegionTickExecutor(Runnable::run, ignored -> {});
            var region = topology.ownerOf(LEFT).orElseThrow();
            var task = topology.submit(LEFT, calls::incrementAndGet);
            topology.maintain(() -> assertThat(executor.tick(region))
                    .isCompletedWithValue(RegionTickExecutor.Result.UNAVAILABLE));

            assertThat(task).isNotDone();
            assertThat(executor.tick(region)).isCompletedWithValue(RegionTickExecutor.Result.EXECUTED);
            assertThat(task).isCompletedWithValue(1);
        }
    }

    @Test
    void simulationFailureFailsTheRegionAndAllUnclaimedTasksWithoutAdvancingItsClock() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var failure = new IllegalStateException("simulation failed");
            var next = new AtomicReference<java.util.concurrent.CompletableFuture<Integer>>();
            var executor = new RegionTickExecutor(Runnable::run, context -> {
                next.set(topology.submit(LEFT, () -> 42));
                throw failure;
            });
            var region = topology.ownerOf(LEFT).orElseThrow();
            var result = executor.tick(region);

            assertThatThrownBy(result::join).hasCause(failure);
            assertThatThrownBy(() -> next.get().join()).hasCause(failure);
            assertThat(region.state()).isEqualTo(TickRegion.State.FAILED);
            assertThat(region.completedTicks()).isZero();
            assertThat(RegionContext.current()).isEmpty();
            assertThat(executor.tick(region)).isCompletedWithValue(RegionTickExecutor.Result.UNAVAILABLE);
        }
    }

    @Test
    void fatalSimulationFailureIsObservableAndRethrownAfterLeaseRelease() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var worker = new QueuedExecutor();
            var fatal = new AssertionError("fatal simulation");
            var executor = new RegionTickExecutor(worker, ignored -> { throw fatal; });
            var region = topology.ownerOf(LEFT).orElseThrow();
            var result = executor.tick(region);

            assertThatThrownBy(worker::runNext).isSameAs(fatal);
            assertThatThrownBy(result::join).hasCause(fatal);
            assertThat(region.state()).isEqualTo(TickRegion.State.FAILED);
            assertThat(RegionContext.current()).isEmpty();
            assertThat(executor.shutdown()).isCompleted();
        }
    }

    @Test
    void aSimulationCannotReleaseItsOwnLeaseMidTick() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var executor = new RegionTickExecutor(Runnable::run, RegionContext::close);
            var region = topology.ownerOf(LEFT).orElseThrow();
            assertThatThrownBy(() -> executor.tick(region).join()).hasCauseInstanceOf(IllegalStateException.class);
            assertThat(region.state()).isEqualTo(TickRegion.State.FAILED);
            assertThat(region.completedTicks()).isZero();
            assertThat(RegionContext.current()).isEmpty();
        }
    }

    @Test
    void workerRejectionEndsTheResultAndAllowsAnotherAttempt() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var attempts = new AtomicInteger();
            var executor = new RegionTickExecutor(action -> {
                if (attempts.getAndIncrement() == 0) {
                    throw new RejectedExecutionException("full");
                }
                action.run();
            }, ignored -> {});
            var region = topology.ownerOf(LEFT).orElseThrow();
            assertThatThrownBy(() -> executor.tick(region).join()).hasCauseInstanceOf(RejectedExecutionException.class);
            assertThat(region.state()).isEqualTo(TickRegion.State.READY);
            assertThat(executor.tick(region)).isCompletedWithValue(RegionTickExecutor.Result.EXECUTED);
        }
    }

    @Test
    void shutdownCancelsQueuedTicksEvenIfTheWorkerNeverRunsThem() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var worker = new QueuedExecutor();
            var calls = new AtomicInteger();
            var executor = new RegionTickExecutor(worker, ignored -> calls.incrementAndGet());
            var region = topology.ownerOf(LEFT).orElseThrow();
            var pending = executor.tick(region);

            assertThat(executor.shutdown()).isCompleted();
            assertThat(pending).isCompletedExceptionally();
            worker.runNext();
            assertThat(calls).hasValue(0);
            assertThat(region.completedTicks()).isZero();
            assertThatThrownBy(() -> executor.tick(region).join()).hasCauseInstanceOf(RejectedExecutionException.class);
        }
    }

    @Test
    void shutdownWaitsForRunningSimulationToReleaseOwnership() throws Exception {
        try (var topology = new TickRegionTopology(1, 1); var worker = Executors.newSingleThreadExecutor()) {
            topology.tryActivate(LEFT);
            var entered = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var executor = new RegionTickExecutor(worker, ignored -> {
                entered.countDown();
                await(release);
            });
            var region = topology.ownerOf(LEFT).orElseThrow();
            var tick = executor.tick(region);
            java.util.concurrent.CompletableFuture<Void> stopped;
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                stopped = executor.shutdown();
                assertThat(stopped).isNotDone();
                assertThat(region.state()).isEqualTo(TickRegion.State.RUNNING);
            } finally {
                release.countDown();
            }
            stopped.get(5, TimeUnit.SECONDS);
            assertThat(tick).isCompletedWithValue(RegionTickExecutor.Result.EXECUTED);
            assertThat(region.state()).isEqualTo(TickRegion.State.READY);
            assertThat(region.completedTicks()).isEqualTo(1);
        }
    }

    @Test
    void completionCallbacksObserveTheReleasedLeaseAndCanScheduleTheNextTick() {
        try (var topology = new TickRegionTopology(1, 1)) {
            topology.tryActivate(LEFT);
            var worker = new QueuedExecutor();
            var executor = new RegionTickExecutor(worker, ignored -> {});
            var region = topology.ownerOf(LEFT).orElseThrow();
            var next = executor.tick(region).thenCompose(ignored -> {
                assertThat(RegionContext.current()).isEmpty();
                assertThat(region.state()).isEqualTo(TickRegion.State.READY);
                return executor.tick(region);
            });
            worker.runNext();
            assertThat(next).isNotDone();
            worker.runNext();
            assertThat(next).isCompletedWithValue(RegionTickExecutor.Result.EXECUTED);
            assertThat(region.completedTicks()).isEqualTo(2);
        }
    }

    @Test
    void identicalNumericRegionIdsFromDifferentWorldsDoNotShareDispatches() {
        try (var first = new TickRegionTopology(1, 1); var second = new TickRegionTopology(1, 1)) {
            first.tryActivate(LEFT);
            second.tryActivate(LEFT);
            var worker = new QueuedExecutor();
            var executor = new RegionTickExecutor(worker, ignored -> {});
            var firstRegion = first.ownerOf(LEFT).orElseThrow();
            var secondRegion = second.ownerOf(LEFT).orElseThrow();
            assertThat(firstRegion.id()).isEqualTo(secondRegion.id());
            var firstTick = executor.tick(firstRegion);
            var secondTick = executor.tick(secondRegion);
            assertThat(worker.queue).hasSize(2);
            worker.runNext();
            worker.runNext();
            assertThat(firstTick).isCompletedWithValue(RegionTickExecutor.Result.EXECUTED);
            assertThat(secondTick).isCompletedWithValue(RegionTickExecutor.Result.EXECUTED);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Simulation release timed out");
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
    }

    private static final class QueuedExecutor implements Executor {
        private final ArrayDeque<Runnable> queue = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            queue.addLast(command);
        }

        private void runNext() {
            queue.removeFirst().run();
        }
    }
}
