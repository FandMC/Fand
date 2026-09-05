package io.fand.server.tick;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class RegionExecutionTest {

    private static final OwnershipCell LEFT = new OwnershipCell(0, 0);
    private static final OwnershipCell BRIDGE = new OwnershipCell(2, 0);
    private static final OwnershipCell RIGHT = new OwnershipCell(4, 0);

    @Test
    void anIndependentTaskBatchCanFinishWhileAnotherRegionIsBlocked() throws Exception {
        try (var topology = new TickRegionTopology(4, 1); var worker = Executors.newSingleThreadExecutor()) {
            topology.tryActivate(LEFT);
            topology.tryActivate(RIGHT);
            var leftStarted = new CountDownLatch(1);
            var releaseLeft = new CountDownLatch(1);
            var left = topology.submit(LEFT, () -> {
                leftStarted.countDown();
                try {
                    if (!releaseLeft.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Left region was not released");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interrupted);
                }
                return 1;
            });
            var right = topology.submit(RIGHT, () -> 2);
            var running = worker.submit(() -> {
                try (var context = topology.tryEnter(topology.ownerOf(LEFT).orElseThrow()).orElseThrow()) {
                    return context.runTasks();
                }
            });
            try {
                assertThat(leftStarted.await(5, TimeUnit.SECONDS)).isTrue();
                try (var context = topology.tryEnter(topology.ownerOf(RIGHT).orElseThrow()).orElseThrow()) {
                    assertThat(context.runTasks()).isEqualTo(1);
                }
                assertThat(right).isCompletedWithValue(2);
                assertThat(left).isNotDone();
                assertThat(topology.tryActivate(BRIDGE)).isEqualTo(TickRegionTopology.Change.BUSY);
            } finally {
                releaseLeft.countDown();
            }
            assertThat(running.get(5, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(left).isCompletedWithValue(1);
            assertThat(topology.tryActivate(BRIDGE)).isEqualTo(TickRegionTopology.Change.APPLIED);
        }
    }

    @Test
    void distinctRegionsCanExecuteConcurrentlyButTheSameRegionCannot() throws Exception {
        try (var topology = new TickRegionTopology(4, 1); var worker = Executors.newSingleThreadExecutor()) {
            topology.tryActivate(LEFT);
            topology.tryActivate(RIGHT);
            var left = topology.ownerOf(LEFT).orElseThrow();
            var right = topology.ownerOf(RIGHT).orElseThrow();
            try (var context = topology.tryEnter(left).orElseThrow()) {
                var parallel = worker.submit(() -> {
                    assertThat(topology.tryEnter(left)).isEmpty();
                    try (var other = topology.tryEnter(right).orElseThrow()) {
                        assertThat(left.state()).isEqualTo(TickRegion.State.RUNNING);
                        assertThat(other.owns(RIGHT)).isTrue();
                        assertThat(other.owns(LEFT)).isFalse();
                        return other.region().id();
                    }
                });
                assertThat(parallel.get(5, TimeUnit.SECONDS)).isEqualTo(right.id());
                assertThat(RegionContext.current()).contains(context);
            }
            assertThat(RegionContext.current()).isEmpty();
        }
    }

    @Test
    void contextsCannotBeUsedOrReleasedFromAnotherThread() throws Exception {
        try (var topology = new TickRegionTopology(4, 1); var worker = Executors.newSingleThreadExecutor()) {
            topology.tryActivate(LEFT);
            var region = topology.ownerOf(LEFT).orElseThrow();
            try (var context = topology.tryEnter(region).orElseThrow()) {
                worker.submit(() -> {
                    assertThatThrownBy(() -> context.owns(LEFT)).isInstanceOf(IllegalStateException.class);
                    assertThatThrownBy(context::close).isInstanceOf(IllegalStateException.class);
                }).get(5, TimeUnit.SECONDS);
                assertThat(region.state()).isEqualTo(TickRegion.State.RUNNING);
                assertThat(context.owns(LEFT)).isTrue();
            }
        }
    }

    @Test
    void oneThreadCannotHoldTwoRegionLeases() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            topology.tryActivate(RIGHT);
            try (var context = topology.tryEnter(topology.ownerOf(LEFT).orElseThrow()).orElseThrow()) {
                assertThatThrownBy(() -> topology.tryEnter(topology.ownerOf(RIGHT).orElseThrow()))
                        .isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> context.requireOwned(RIGHT)).isInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    void closedLeaseCannotReleaseALaterExecutionOfTheSameRegion() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var region = topology.ownerOf(LEFT).orElseThrow();
            var old = topology.tryEnter(region).orElseThrow();
            old.close();
            try (var current = topology.tryEnter(region).orElseThrow()) {
                old.close();
                assertThatThrownBy(() -> old.owns(LEFT)).isInstanceOf(IllegalStateException.class);
                assertThat(region.state()).isEqualTo(TickRegion.State.RUNNING);
                assertThat(RegionContext.current()).contains(current);
            }
        }
    }

    @Test
    void pendingTasksFollowMergedOwnershipInSubmissionOrder() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            topology.tryActivate(RIGHT);
            var calls = new ArrayList<String>();
            var first = topology.submit(RIGHT, () -> calls.add("right"));
            var second = topology.submit(LEFT, () -> calls.add("left"));
            topology.tryActivate(BRIDGE);
            var merged = topology.ownerOf(BRIDGE).orElseThrow();
            var third = topology.submit(BRIDGE, () -> RegionContext.current().orElseThrow().region());

            try (var context = topology.tryEnter(merged).orElseThrow()) {
                assertThat(context.runTasks()).isEqualTo(3);
            }

            assertThat(calls).containsExactly("right", "left");
            assertThat(first).isCompleted();
            assertThat(second).isCompleted();
            assertThat(third.join()).isSameAs(merged);
        }
    }

    @Test
    void pendingTasksStayWithTheirCellsAfterSplit() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            topology.tryActivate(BRIDGE);
            topology.tryActivate(RIGHT);
            var left = topology.submit(LEFT, () -> RegionContext.current().orElseThrow().region());
            var right = topology.submit(RIGHT, () -> RegionContext.current().orElseThrow().region());
            var removed = topology.submit(BRIDGE, () -> 42);

            topology.tryDeactivate(BRIDGE);
            var leftRegion = topology.ownerOf(LEFT).orElseThrow();
            var rightRegion = topology.ownerOf(RIGHT).orElseThrow();
            try (var context = topology.tryEnter(leftRegion).orElseThrow()) {
                assertThat(context.runTasks()).isEqualTo(1);
            }
            assertThat(left.join()).isSameAs(leftRegion);
            assertThat(right).isNotDone();
            assertThat(removed).isCancelled();
            try (var context = topology.tryEnter(rightRegion).orElseThrow()) {
                context.runTasks();
            }
            assertThat(right.join()).isSameAs(rightRegion);
        }
    }

    @Test
    void reactivationFromRetirementCallbackDoesNotReviveOldWork() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var calls = new AtomicInteger();
            var old = topology.submit(LEFT, calls::incrementAndGet);
            var replacement = new AtomicReference<CompletableFuture<Integer>>();
            old.whenComplete((value, failure) -> {
                topology.tryActivate(LEFT);
                replacement.set(topology.submit(LEFT, () -> 42));
            });

            topology.tryDeactivate(LEFT);
            try (var context = topology.tryEnter(topology.ownerOf(LEFT).orElseThrow()).orElseThrow()) {
                context.runTasks();
            }

            assertThat(old).isCancelled();
            assertThat(calls).hasValue(0);
            assertThat(replacement.get()).isCompletedWithValue(42);
        }
    }

    @Test
    void tasksSubmittedDuringDrainWaitForTheNextExecution() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var result = topology.submit(LEFT, () -> topology.submit(LEFT, () -> 42));
            var region = topology.ownerOf(LEFT).orElseThrow();
            try (var context = topology.tryEnter(region).orElseThrow()) {
                assertThat(context.runTasks()).isEqualTo(1);
                assertThat(result.join()).isNotDone();
                assertThatThrownBy(context::runTasks).isInstanceOf(IllegalStateException.class);
            }
            try (var context = topology.tryEnter(region).orElseThrow()) {
                assertThat(context.runTasks()).isEqualTo(1);
            }
            assertThat(result.join()).isCompletedWithValue(42);
        }
    }

    @Test
    void cancelledTaskDoesNotExecute() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var result = topology.submit(LEFT, () -> 42);
            result.cancel(true);

            try (var context = topology.tryEnter(topology.ownerOf(LEFT).orElseThrow()).orElseThrow()) {
                assertThat(context.runTasks()).isZero();
            }

            assertThat(result).isCancelled();
        }
    }

    @Test
    void taskCannotReleaseTheLeaseWhileItsBatchIsExecuting() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var illegalClose = topology.submit(LEFT, () -> {
                RegionContext.current().orElseThrow().close();
                return 1;
            });
            var next = topology.submit(LEFT, () -> RegionContext.current().orElseThrow().owns(LEFT));

            try (var context = topology.tryEnter(topology.ownerOf(LEFT).orElseThrow()).orElseThrow()) {
                context.runTasks();
            }

            assertThatThrownBy(illegalClose::join).hasCauseInstanceOf(IllegalStateException.class);
            assertThat(next).isCompletedWithValue(true);
        }
    }

    @Test
    void fatalFailureEndsAllResultsAndPreventsFurtherRegionExecution() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var fatal = new AssertionError("fatal task");
            var submittedDuringRun = new AtomicReference<CompletableFuture<Integer>>();
            var first = topology.submit(LEFT, () -> {
                submittedDuringRun.set(topology.submit(LEFT, () -> 7));
                throw fatal;
            });
            var next = topology.submit(LEFT, () -> 42);
            var region = topology.ownerOf(LEFT).orElseThrow();

            try (var context = topology.tryEnter(region).orElseThrow()) {
                assertThatThrownBy(context::runTasks).isSameAs(fatal);
                assertThatThrownBy(() -> context.owns(LEFT)).hasCause(fatal);
            }

            assertThatThrownBy(first::join).hasCause(fatal);
            assertThatThrownBy(next::join).hasCause(fatal);
            assertThatThrownBy(() -> submittedDuringRun.get().join()).hasCause(fatal);
            assertThat(region.state()).isEqualTo(TickRegion.State.FAILED);
            assertThat(topology.tryEnter(region)).isEmpty();
            assertThatThrownBy(() -> topology.submit(LEFT, () -> 43).join())
                    .hasCauseInstanceOf(RejectedExecutionException.class);
            assertThatThrownBy(() -> topology.tryActivate(BRIDGE)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> topology.tryDeactivate(LEFT)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void ordinaryActionFailureDoesNotDiscardTheRemainingBatch() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var failed = topology.submit(LEFT, () -> { throw new IllegalArgumentException("invalid"); });
            var next = topology.submit(LEFT, () -> 42);
            var region = topology.ownerOf(LEFT).orElseThrow();
            try (var context = topology.tryEnter(region).orElseThrow()) {
                assertThat(context.runTasks()).isEqualTo(2);
            }

            assertThatThrownBy(failed::join).hasRootCauseMessage("invalid");
            assertThat(next).isCompletedWithValue(42);
            assertThat(region.state()).isEqualTo(TickRegion.State.READY);
        }
    }

    @Test
    void closeRequiresAllLeasesToBeReleasedAndRetiresTheWorldInstance() {
        try (var topology = new TickRegionTopology(4, 1)) {
            topology.tryActivate(LEFT);
            var region = topology.ownerOf(LEFT).orElseThrow();
            var pending = topology.submit(LEFT, () -> 42);
            try (var context = topology.tryEnter(region).orElseThrow()) {
                assertThatThrownBy(topology::close).isInstanceOf(IllegalStateException.class);
                assertThat(pending).isNotDone();
            }

            topology.close();

            assertThat(pending).isCancelled();
            assertThat(topology.regions()).isEmpty();
            assertThat(region.state()).isEqualTo(TickRegion.State.RETIRED);
            assertThat(topology.tryEnter(region)).isEmpty();
            assertThatThrownBy(() -> topology.tryActivate(LEFT)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void submissionsToInactiveOrClosedCellsFailPromptly() {
        try (var topology = new TickRegionTopology(4, 1)) {
            assertThatThrownBy(() -> topology.submit(LEFT, () -> 42).join())
                    .hasCauseInstanceOf(RejectedExecutionException.class);
            topology.tryActivate(LEFT);
            assertThatThrownBy(() -> topology.submit(new OwnershipCell(1, 1), () -> 42).join())
                    .hasCauseInstanceOf(RejectedExecutionException.class);
            topology.close();
            assertThatThrownBy(() -> topology.submit(LEFT, () -> 42).join())
                    .hasCauseInstanceOf(RejectedExecutionException.class);
        }
    }

    @Test
    void retirementCompletionCallbacksDoNotRunUnderTheMetadataLock() throws Exception {
        try (var topology = new TickRegionTopology(4, 1); var worker = Executors.newSingleThreadExecutor()) {
            topology.tryActivate(LEFT);
            var pending = topology.submit(LEFT, () -> 42);
            var callback = pending.handle((value, failure) -> {
                try {
                    return worker.submit(topology::activeCells).get(5, TimeUnit.SECONDS);
                } catch (Exception unexpected) {
                    throw new AssertionError(unexpected);
                }
            });

            topology.tryDeactivate(LEFT);

            assertThat(callback.join()).isEmpty();
        }
    }

    @Test
    void closingAndConcurrentSubmissionCannotLeaveUnfinishedResults() throws Exception {
        try (var topology = new TickRegionTopology(4, 1); var worker = Executors.newSingleThreadExecutor()) {
            topology.tryActivate(LEFT);
            var submissions = worker.submit(() -> {
                var results = new ArrayList<CompletableFuture<Integer>>();
                for (int index = 0; index < 500; index++) {
                    results.add(topology.submit(LEFT, () -> 42));
                }
                return results;
            });

            topology.close();

            assertThat(submissions.get(5, TimeUnit.SECONDS)).allMatch(CompletableFuture::isDone);
        }
    }
}
