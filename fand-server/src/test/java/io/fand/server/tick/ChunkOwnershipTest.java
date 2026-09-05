package io.fand.server.tick;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkOwnershipTest {

    @Test
    void aCellStaysActiveUntilItsLastHolderActuallyRetires() {
        var ownership = new ChunkOwnership<String>(4, 1);
        var first = ownership.register(0, 0, "first");
        var second = ownership.register(3, 2, "second");
        ownership.drain(20, 20);
        var topology = ownership.topology();
        var region = topology.ownerOf(first.cell()).orElseThrow();
        var surviving = second.submit(value -> value);

        var retired = first.retire();
        ownership.drain(20, 20);

        assertThat(retired).isCompleted();
        assertThat(ownership.installedChunks()).isEqualTo(1);
        assertThat(ownership.chunksIn(first.cell())).isEqualTo(1);
        assertThat(topology.ownerOf(first.cell())).containsSame(region);
        assertThat(surviving).isNotDone();
        try (var context = topology.tryEnter(region).orElseThrow()) {
            context.runTasks();
        }
        assertThat(surviving).isCompletedWithValue("second");

        second.retire();
        ownership.drain(20, 20);
        assertThat(topology.activeCells()).isEmpty();
        assertThat(ownership.chunksIn(first.cell())).isZero();
        assertThat(region.state()).isEqualTo(TickRegion.State.RETIRED);
    }

    @Test
    void replacementRejectsOldResultsEvenWhileTheCellRemainsActive() {
        var ownership = new ChunkOwnership<AtomicInteger>(4, 1);
        var holder = new AtomicInteger();
        var old = ownership.register(0, 0, holder);
        ownership.register(1, 0, new AtomicInteger());
        ownership.drain(20, 20);
        var oldResult = old.submit(AtomicInteger::incrementAndGet);
        var region = ownership.topology().ownerOf(old.cell()).orElseThrow();

        // Even reuse of the same holder object creates a different lifetime.
        var replacement = ownership.register(0, 0, holder);
        ownership.drain(20, 20);
        var staleRemoval = old.retire();
        ownership.drain(20, 20);
        var currentResult = replacement.submit(AtomicInteger::incrementAndGet);
        try (var context = ownership.topology().tryEnter(region).orElseThrow()) {
            context.runTasks();
        }

        assertThat(oldResult).isCancelled();
        assertThat(staleRemoval).isCompleted();
        assertThat(old.state()).isEqualTo(ChunkOwnership.Handle.State.RETIRED);
        assertThat(replacement.state()).isEqualTo(ChunkOwnership.Handle.State.ACTIVE);
        assertThat(currentResult).isCompletedWithValue(1);
        assertThat(ownership.installedChunks()).isEqualTo(2);
        assertThatThrownBy(() -> old.submit(AtomicInteger::incrementAndGet).join())
                .hasCauseInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void concurrentSubmissionAndReplacementCannotLeaveOldTasksPending() throws Exception {
        var ownership = new ChunkOwnership<AtomicInteger>(4, 1);
        var writes = new AtomicInteger();
        var old = ownership.register(0, 0, writes);
        ownership.drain(20, 20);
        try (var worker = Executors.newSingleThreadExecutor()) {
            var submissions = worker.submit(() -> {
                var results = new ArrayList<CompletableFuture<Integer>>();
                for (int index = 0; index < 1000; index++) {
                    results.add(old.submit(AtomicInteger::incrementAndGet));
                }
                return results;
            });
            ownership.register(0, 0, new AtomicInteger());
            ownership.drain(20, 20);
            var results = submissions.get(5, TimeUnit.SECONDS);
            runTasks(ownership, old.cell());

            assertThat(results).allMatch(CompletableFuture::isDone);
            assertThat(results).allMatch(CompletableFuture::isCompletedExceptionally);
            assertThat(writes).hasValue(0);
        }
    }

    @Test
    void delayedHolderTasksAreCancelledEvenWhenAnotherHolderKeepsTheCellActive() {
        var ownership = new ChunkOwnership<String>(4, 1);
        var old = ownership.register(0, 0, "old");
        var survivor = ownership.register(1, 0, "survivor");
        ownership.drain(20, 20);
        var delayed = old.submitAfter(1000, value -> value);
        var surviving = survivor.submitAfter(2, value -> value);
        old.retire();
        ownership.drain(20, 20);

        assertThat(delayed).isCancelled();
        var executor = new RegionTickExecutor(Runnable::run, ignored -> {});
        var region = ownership.topology().ownerOf(survivor.cell()).orElseThrow();
        executor.tick(region).join();
        assertThat(surviving).isNotDone();
        executor.tick(region).join();
        assertThat(surviving).isCompletedWithValue("survivor");
    }

    @Test
    void retirementBeforeAdmissionNeverActivatesTheCell() {
        var ownership = new ChunkOwnership<String>(4, 1);
        var handle = ownership.register(-1, -5, "pending");
        var ready = handle.ready();
        var retired = handle.retire();
        ownership.drain(20, 20);

        assertThat(handle.cell()).isEqualTo(new OwnershipCell(-1, -2));
        assertThatThrownBy(ready::join).hasCauseInstanceOf(CancellationException.class);
        assertThat(retired).isCompleted();
        assertThat(ownership.topology().regions()).isEmpty();
        assertThat(ownership.installedChunks()).isZero();
    }

    @Test
    void onlyTheLatestPendingRegistrationIsInstalled() {
        var ownership = new ChunkOwnership<String>(4, 1);
        var first = ownership.register(0, 0, "old");
        var latest = ownership.register(0, 0, "latest");
        ownership.drain(20, 20);

        assertThat(first.ready()).isCompletedExceptionally();
        assertThat(latest.ready()).isCompleted();
        assertThat(ownership.installedChunks()).isEqualTo(1);
        var result = latest.submit(value -> value);
        runTasks(ownership, latest.cell());
        assertThat(result).isCompletedWithValue("latest");
    }

    @Test
    void retiringAReplacementDoesNotRestoreTheOldLifetime() {
        var ownership = new ChunkOwnership<String>(4, 1);
        var old = ownership.register(0, 0, "old");
        ownership.drain(20, 20);
        var replacement = ownership.register(0, 0, "new");
        ownership.drain(1, 1);
        replacement.retire();
        ownership.drain(20, 20);

        assertThat(old.state()).isEqualTo(ChunkOwnership.Handle.State.RETIRED);
        assertThat(replacement.state()).isEqualTo(ChunkOwnership.Handle.State.RETIRED);
        assertThat(ownership.installedChunks()).isZero();
    }

    @Test
    void busyMergesRotateBehindIndependentChangesWithinTheBudget() throws Exception {
        var ownership = new ChunkOwnership<String>(1, 1);
        var left = ownership.register(0, 0, "left");
        ownership.register(4, 0, "right");
        ownership.drain(20, 20);
        try (var blocked = new BlockedRegion(ownership.topology(), left.cell())) {
            var bridge = ownership.register(2, 0, "bridge");
            var far = ownership.register(20, 0, "far");
            var firstPass = ownership.drain(2, 1);
            assertThat(firstPass.busy()).isEqualTo(1);
            assertThat(firstPass.pendingChanges()).isEqualTo(2);
            assertThat(bridge.ready()).isNotDone();

            var secondPass = ownership.drain(2, 1);
            assertThat(secondPass.applied()).isEqualTo(1);
            assertThat(far.ready()).isCompleted();
            assertThat(bridge.ready()).isNotDone();
            assertThat(ownership.topology().regions()).hasSize(3);
        }
        ownership.drain(20, 20);
        assertThat(ownership.topology().regions()).hasSize(2);
        assertThat(ownership.installedChunks()).isEqualTo(4);
    }

    @Test
    void addingOrRemovingWithinOneCellAlsoWaitsForItsExecutionLease() throws Exception {
        var ownership = new ChunkOwnership<String>(4, 1);
        var first = ownership.register(0, 0, "first");
        ownership.drain(20, 20);
        var second = ownership.register(1, 1, "second");
        CompletableFuture<Void> retired;
        try (var blocked = new BlockedRegion(ownership.topology(), first.cell())) {
            retired = first.retire();
            var pass = ownership.drain(20, 20);
            assertThat(pass.busy()).isEqualTo(2);
            assertThat(first.state()).isEqualTo(ChunkOwnership.Handle.State.ACTIVE);
            assertThat(second.ready()).isNotDone();
            assertThat(retired).isNotDone();
            assertThat(ownership.chunksIn(first.cell())).isEqualTo(1);
        }
        ownership.drain(20, 20);
        assertThat(second.ready()).isCompleted();
        assertThat(retired).isCompleted();
        assertThat(ownership.chunksIn(first.cell())).isEqualTo(1);
    }

    @Test
    void inboxAndChangeBudgetsAreIndependent() {
        var ownership = new ChunkOwnership<String>(1, 1);
        var first = ownership.register(0, 0, "one");
        var second = ownership.register(10, 0, "two");
        var third = ownership.register(20, 0, "three");

        var pass = ownership.drain(2, 1);
        assertThat(pass.requests()).isEqualTo(2);
        assertThat(pass.applied()).isEqualTo(1);
        assertThat(pass.queuedRequests()).isEqualTo(1);
        assertThat(pass.pendingChanges()).isEqualTo(1);
        assertThat(first.ready()).isCompleted();
        assertThat(second.ready()).isNotDone();
        assertThat(third.ready()).isNotDone();
        ownership.drain(1, 2);
        assertThat(second.ready()).isCompleted();
        assertThat(third.ready()).isCompleted();
    }

    @Test
    void retirementRequestPreventsQueuedPublicationBeforeTheNextMaintenancePass() {
        var ownership = new ChunkOwnership<AtomicInteger>(4, 1);
        var writes = new AtomicInteger();
        var handle = ownership.register(0, 0, writes);
        ownership.drain(20, 20);
        var result = handle.submit(AtomicInteger::incrementAndGet);
        handle.retire();

        runTasks(ownership, handle.cell());

        assertThat(result).isCancelled();
        assertThat(writes).hasValue(0);
        ownership.drain(20, 20);
        assertThat(ownership.installedChunks()).isZero();
    }

    @Test
    void cancellingObservationFuturesDoesNotCancelLifecycleChanges() {
        var ownership = new ChunkOwnership<String>(4, 1);
        var handle = ownership.register(0, 0, "holder");
        handle.ready().cancel(false);
        ownership.drain(20, 20);
        assertThat(handle.ready()).isCompleted();
        handle.retire().cancel(false);
        ownership.drain(20, 20);
        assertThat(handle.retire()).isCompleted();
        assertThat(ownership.installedChunks()).isZero();
    }

    @Test
    void completionCallbacksRunAfterPublicationAndCanEnterFromAnotherThread() throws Exception {
        var ownership = new ChunkOwnership<String>(4, 1);
        var first = ownership.register(0, 0, "first");
        try (var worker = Executors.newSingleThreadExecutor()) {
            var observed = first.ready().thenApply(ignored -> {
                try {
                    return worker.submit(() -> {
                        var region = ownership.topology().ownerOf(first.cell()).orElseThrow();
                        try (var context = ownership.topology().tryEnter(region).orElseThrow()) {
                            return context.owns(first.cell());
                        }
                    }).get(5, TimeUnit.SECONDS);
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
            ownership.drain(20, 20);
            assertThat(observed).isCompletedWithValue(true);
        }
    }

    @Test
    void retirementCallbackCanRegisterANewLifetimeButCannotReenterThePass() {
        var ownership = new ChunkOwnership<String>(4, 1);
        var first = ownership.register(0, 0, "first");
        ownership.drain(20, 20);
        var pending = first.submit(value -> value);
        var replacement = new AtomicReference<ChunkOwnership.Handle<String>>();
        var callback = pending.whenComplete((value, failure) -> {
            assertThat(first.state()).isEqualTo(ChunkOwnership.Handle.State.RETIRED);
            assertThat(ownership.installedChunks()).isZero();
            assertThatThrownBy(() -> ownership.drain(20, 20)).isInstanceOf(IllegalStateException.class);
            replacement.set(ownership.register(0, 0, "replacement"));
        });
        first.retire();
        ownership.drain(20, 20);
        assertThat(callback).isCompletedExceptionally();
        assertThat(replacement.get().ready()).isNotDone();
        ownership.drain(20, 20);
        assertThat(replacement.get().ready()).isCompleted();
    }

    @Test
    void shutdownStopsAdmissionAndWaitsForTheActiveLeaseWithoutInterruptingIt() throws Exception {
        var ownership = new ChunkOwnership<String>(4, 1);
        var handle = ownership.register(0, 0, "holder");
        ownership.drain(20, 20);
        var pending = handle.submit(value -> value);
        var topology = ownership.topology();
        CompletableFuture<Void> stopped;
        try (var blocked = new BlockedRegion(topology, handle.cell())) {
            stopped = ownership.shutdown();
            ownership.drain(20, 20);
            assertThat(stopped).isNotDone();
            assertThat(handle.state()).isEqualTo(ChunkOwnership.Handle.State.ACTIVE);
            assertThat(topology.tryEnter(topology.ownerOf(handle.cell()).orElseThrow())).isEmpty();
            assertThat(ownership.register(1, 0, "late").ready()).isCompletedExceptionally();
            assertThat(handle.submit(value -> value)).isCompletedExceptionally();
            assertThat(pending).isNotDone();
        }
        ownership.drain(20, 20);
        assertThat(stopped).isCompleted();
        assertThat(pending).isCancelled();
        assertThat(handle.state()).isEqualTo(ChunkOwnership.Handle.State.RETIRED);
        assertThat(ownership.topology().regions()).isEmpty();
        assertThat(ownership.drain(20, 20).closed()).isTrue();
    }

    @Test
    void shutdownAndConcurrentRegistrationsLeaveNoUnfinishedLifetimeResults() throws Exception {
        var ownership = new ChunkOwnership<Integer>(4, 1);
        try (var worker = Executors.newSingleThreadExecutor()) {
            var handles = worker.submit(() -> {
                var result = new ArrayList<ChunkOwnership.Handle<Integer>>();
                for (int index = 0; index < 500; index++) {
                    result.add(ownership.register(index, 0, index));
                }
                return result;
            });
            var stopped = ownership.shutdown();
            var registered = handles.get(5, TimeUnit.SECONDS);
            for (int pass = 0; pass < 100 && !stopped.isDone(); pass++) {
                ownership.drain(10, 10);
            }
            assertThat(stopped).isCompleted();
            assertThat(registered).allMatch(handle -> handle.ready().isDone());
            assertThat(registered).allMatch(handle -> handle.retire().isDone());
        }
    }

    @Test
    void failedRegionsRejectNewHoldersAndStillAllowWorldShutdown() {
        var ownership = new ChunkOwnership<String>(4, 1);
        var current = ownership.register(0, 0, "current");
        ownership.drain(20, 20);
        var region = ownership.topology().ownerOf(current.cell()).orElseThrow();
        try (var context = ownership.topology().tryEnter(region).orElseThrow()) {
            assertThatThrownBy(() -> context.runTick(ignored -> {
                throw new IllegalStateException("simulation failed");
            })).hasMessage("simulation failed");
        }
        var replacement = ownership.register(0, 0, "replacement");
        assertThatThrownBy(() -> ownership.drain(20, 20)).hasMessageContaining("failed region");
        assertThat(replacement.ready()).isCompletedExceptionally();
        assertThat(ownership.installedChunks()).isEqualTo(1);
        var stopped = ownership.shutdown();
        ownership.drain(20, 20);
        assertThat(stopped).isCompleted();
        assertThat(current.retire()).isCompleted();
    }

    @Test
    void invalidGeometryFailsItsRequestWithoutDroppingTheRestOfTheInbox() {
        var ownership = new ChunkOwnership<String>(1, 1);
        var invalid = ownership.register(Integer.MAX_VALUE, 0, "invalid");
        var valid = ownership.register(0, 0, "valid");
        assertThatThrownBy(() -> ownership.drain(1, 1)).isInstanceOf(ArithmeticException.class);
        assertThat(invalid.ready()).isCompletedExceptionally();
        assertThat(valid.ready()).isNotDone();
        ownership.drain(20, 20);
        assertThat(valid.ready()).isCompleted();
        assertThat(ownership.installedChunks()).isEqualTo(1);
    }

    @Test
    void onlyTheControlThreadCanDrainOrReadItsMutableAccounting() throws Exception {
        var ownership = new ChunkOwnership<String>(4, 1);
        try (var worker = Executors.newSingleThreadExecutor()) {
            worker.submit(() -> {
                ownership.register(0, 0, "allowed");
                assertThatThrownBy(() -> ownership.drain(20, 20)).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(ownership::installedChunks).isInstanceOf(IllegalStateException.class);
            }).get(5, TimeUnit.SECONDS);
        }
        assertThatThrownBy(() -> ownership.drain(0, 1)).isInstanceOf(IllegalArgumentException.class);
        ownership.drain(20, 20);
        assertThat(ownership.installedChunks()).isEqualTo(1);
    }

    private static void runTasks(ChunkOwnership<?> ownership, OwnershipCell cell) {
        var topology = ownership.topology();
        try (var context = topology.tryEnter(topology.ownerOf(cell).orElseThrow()).orElseThrow()) {
            context.runTasks();
        }
    }

    private static final class BlockedRegion implements AutoCloseable {
        private final ExecutorService worker = Executors.newSingleThreadExecutor();
        private final CountDownLatch release = new CountDownLatch(1);
        private final Future<?> running;

        private BlockedRegion(TickRegionTopology topology, OwnershipCell cell) throws Exception {
            var entered = new CountDownLatch(1);
            running = worker.submit(() -> {
                try (var context = topology.tryEnter(topology.ownerOf(cell).orElseThrow()).orElseThrow()) {
                    entered.countDown();
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new AssertionError("Lease release timed out");
                    }
                }
                return null;
            });
            if (!entered.await(5, TimeUnit.SECONDS)) {
                release.countDown();
                worker.close();
                throw new AssertionError("Lease entry timed out");
            }
        }

        @Override
        public void close() throws Exception {
            release.countDown();
            try {
                running.get(5, TimeUnit.SECONDS);
            } finally {
                worker.close();
            }
        }
    }
}
