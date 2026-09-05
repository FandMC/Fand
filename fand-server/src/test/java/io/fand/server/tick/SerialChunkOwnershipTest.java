package io.fand.server.tick;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SerialChunkOwnershipTest {

    @Test
    void publicationRunsOnTheControlThreadAfterBackgroundPreparation() throws Exception {
        var loop = new QueuedExecutor();
        try (var ownership = new SerialChunkOwnership<String>(loop, 4, 1);
             var worker = Executors.newSingleThreadExecutor()) {
            var lifetime = ownership.attach(0, 0, "holder");
            var prepared = new CompletableFuture<String>();
            var controlThread = Thread.currentThread();
            var published = lifetime.publish(prepared, (data, failure) -> {
                assertThat(Thread.currentThread()).isSameAs(controlThread);
                assertThat(RegionContext.current()).isEmpty();
                assertThat(failure).isNull();
                return data;
            });
            worker.submit(() -> prepared.complete("loaded")).get(5, TimeUnit.SECONDS);

            assertThat(published).isNotDone();
            loop.drain();
            assertThat(published).isCompletedWithValue("loaded");
        }
    }

    @Test
    void cellSurvivesOneHolderRetirementAndSplitsWhenItsBridgeIsRemoved() {
        try (var ownership = new SerialChunkOwnership<String>(Runnable::run, 4, 1)) {
            var left = ownership.attach(0, 0, "left");
            var neighbor = ownership.attach(1, 1, "neighbor");
            ownership.attach(16, 0, "right");
            assertThat(ownership.snapshot()).isEqualTo(new SerialChunkOwnership.Snapshot(3, 2, 2, false));
            var bridge = ownership.attach(8, 0, "bridge");
            assertThat(ownership.snapshot().regions()).isEqualTo(1);
            ownership.detach(left);
            assertThat(neighbor.active()).isTrue();
            assertThat(ownership.snapshot().activeCells()).isEqualTo(3);
            ownership.detach(bridge);
            assertThat(ownership.snapshot()).isEqualTo(new SerialChunkOwnership.Snapshot(2, 2, 2, false));
        }
    }

    @Test
    void simulationCellsResolveCurrentOwnersAcrossMergeSplitAndRetirement() {
        try (var ownership = new SerialChunkOwnership<String>(Runnable::run, 4, 1)) {
            assertThat(ownership.cellAtChunk(-1, -5)).isEqualTo(new OwnershipCell(-1, -2));
            var leftCell = ownership.cellAtChunk(0, 0);
            var rightCell = ownership.cellAtChunk(16, 0);
            assertThat(ownership.regionIdAt(leftCell)).isEmpty();
            var left = ownership.attach(0, 0, "left");
            ownership.attach(16, 0, "right");
            assertThat(ownership.regionIdAt(leftCell)).isPresent().isNotEqualTo(ownership.regionIdAt(rightCell));

            var bridge = ownership.attach(8, 0, "bridge");
            assertThat(ownership.regionIdAt(leftCell)).isEqualTo(ownership.regionIdAt(rightCell));
            ownership.detach(bridge);
            assertThat(ownership.regionIdAt(leftCell)).isPresent().isNotEqualTo(ownership.regionIdAt(rightCell));
            ownership.detach(left);
            assertThat(ownership.regionIdAt(leftCell)).isEmpty();
            assertThat(ownership.regionIdAt(rightCell)).isPresent();
        }
    }

    @Test
    void replacementCancelsOldQueuedPublicationWithoutTouchingTheNewLifetime() {
        var loop = new QueuedExecutor();
        try (var ownership = new SerialChunkOwnership<String>(loop, 4, 1)) {
            var old = ownership.attach(0, 0, "holder");
            var calls = new AtomicInteger();
            var pending = old.publish(CompletableFuture.completedFuture("old"), (data, failure) -> calls.incrementAndGet());
            var replacement = ownership.attach(0, 0, "holder");
            ownership.detach(old);
            var current = replacement.publish(CompletableFuture.completedFuture("current"), (data, failure) -> data);
            loop.drain();

            assertRetired(pending);
            assertThat(current).isCompletedWithValue("current");
            assertThat(calls).hasValue(0);
            assertThat(ownership.snapshot().holders()).isEqualTo(1);
        }
    }

    @Test
    void retirementEndsPendingIoWithoutWaitingForItOrCallingRecovery() {
        var loop = new QueuedExecutor();
        try (var ownership = new SerialChunkOwnership<String>(loop, 4, 1)) {
            var lifetime = ownership.attach(0, 0, "holder");
            var prepared = new CompletableFuture<String>();
            var recoveries = new AtomicInteger();
            var result = lifetime.publish(prepared, (data, failure) -> recoveries.incrementAndGet());
            ownership.detach(lifetime);

            assertRetired(result);
            assertThat(prepared).isNotDone();
            prepared.completeExceptionally(new IOException("late IO error"));
            loop.drain();
            assertThat(recoveries).hasValue(0);
            assertThat(loop.queue).isEmpty();
        }
    }

    @Test
    void ordinaryIoFailureStillReachesTheActiveLifetimesRecoveryHandler() {
        var loop = new QueuedExecutor();
        try (var ownership = new SerialChunkOwnership<String>(loop, 4, 1)) {
            var lifetime = ownership.attach(0, 0, "holder");
            var ioFailure = new IOException("read failed");
            var published = lifetime.publish(CompletableFuture.<String>failedFuture(ioFailure), (data, failure) -> {
                assertThat(data).isNull();
                assertThat(failure).isSameAs(ioFailure);
                return "recovered";
            });
            loop.drain();
            assertThat(published).isCompletedWithValue("recovered");
        }
    }

    @Test
    void worldCloseCancelsQueuedAndPreparingPublicationsWithoutDrainingTheEventLoop() {
        var loop = new QueuedExecutor();
        var ownership = new SerialChunkOwnership<String>(loop, 4, 1);
        var lifetime = ownership.attach(0, 0, "holder");
        var calls = new AtomicInteger();
        var queued = lifetime.publish(CompletableFuture.completedFuture(1), (data, failure) -> calls.incrementAndGet());
        var waiting = lifetime.publish(new CompletableFuture<Integer>(), (data, failure) -> calls.incrementAndGet());

        ownership.close();
        ownership.close();

        assertRetired(queued);
        assertRetired(waiting);
        assertRetired(lifetime.publish(CompletableFuture.completedFuture(1), (data, failure) -> 1));
        assertThat(ownership.snapshot()).isEqualTo(new SerialChunkOwnership.Snapshot(0, 0, 0, true));
        loop.drain();
        assertThat(calls).hasValue(0);
        assertThatThrownBy(() -> ownership.attach(0, 0, "late")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aWorldCannotDetachALifetimeFromAnotherWorldInstance() {
        try (var oldWorld = new SerialChunkOwnership<String>(Runnable::run, 4, 1);
             var newWorld = new SerialChunkOwnership<String>(Runnable::run, 4, 1)) {
            var old = oldWorld.attach(0, 0, "old");
            var current = newWorld.attach(0, 0, "new");
            assertThatThrownBy(() -> newWorld.detach(old)).isInstanceOf(IllegalArgumentException.class);
            oldWorld.close();
            assertThat(current.active()).isTrue();
        }
    }

    @Test
    void lifecycleChangesCannotRunOnABackgroundThread() throws Exception {
        try (var ownership = new SerialChunkOwnership<String>(Runnable::run, 4, 1);
             var worker = Executors.newSingleThreadExecutor()) {
            var lifetime = ownership.attach(0, 0, "holder");
            worker.submit(() -> {
                assertThatThrownBy(() -> ownership.attach(1, 1, "bad")).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> ownership.detach(lifetime)).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(ownership::close).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> ownership.regionIdAt(new OwnershipCell(0, 0))).isInstanceOf(IllegalStateException.class);
            }).get(5, TimeUnit.SECONDS);
            assertThat(lifetime.active()).isTrue();
        }
    }

    @Test
    void executorRejectionAndWrongThreadPublicationEndTheirResults() throws Exception {
        try (var rejected = new SerialChunkOwnership<String>(action -> {
            throw new RejectedExecutionException("closed loop");
        }, 4, 1)) {
            var lifetime = rejected.attach(0, 0, "holder");
            var result = lifetime.publish(CompletableFuture.completedFuture(1), (data, failure) -> 1);
            assertThatThrownBy(result::join).hasCauseInstanceOf(RejectedExecutionException.class);
        }
        try (var worker = Executors.newSingleThreadExecutor();
             var wrongThread = new SerialChunkOwnership<String>(worker, 4, 1)) {
            var lifetime = wrongThread.attach(0, 0, "holder");
            var result = lifetime.publish(CompletableFuture.completedFuture(1), (data, failure) -> 1);
            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void callerCancellationSkipsPublicationWithoutRetiringTheHolder() {
        var loop = new QueuedExecutor();
        try (var ownership = new SerialChunkOwnership<String>(loop, 4, 1)) {
            var lifetime = ownership.attach(0, 0, "holder");
            var calls = new AtomicInteger();
            var result = lifetime.publish(CompletableFuture.completedFuture(1), (data, failure) -> calls.incrementAndGet());
            result.cancel(false);
            loop.drain();
            assertThat(calls).hasValue(0);
            assertThat(lifetime.active()).isTrue();
        }
    }

    @Test
    void publicationAlreadyEnteredCanFinishWhenItsCallbackClosesTheWorld() {
        var loop = new QueuedExecutor();
        var ownership = new SerialChunkOwnership<String>(loop, 4, 1);
        var lifetime = ownership.attach(0, 0, "holder");
        var result = lifetime.publish(CompletableFuture.completedFuture(1), (data, failure) -> {
            ownership.close();
            return 42;
        });
        loop.drain();
        assertThat(result).isCompletedWithValue(42);
        assertThat(lifetime.active()).isFalse();
    }

    @Test
    void concurrentPublicationAndRetirementLeaveNoUnfinishedResults() throws Exception {
        var loop = new QueuedExecutor();
        try (var ownership = new SerialChunkOwnership<String>(loop, 4, 1);
             var worker = Executors.newSingleThreadExecutor()) {
            var lifetime = ownership.attach(0, 0, "holder");
            var calls = new AtomicInteger();
            var pending = worker.submit(() -> {
                var results = new ArrayList<CompletableFuture<Integer>>();
                for (int index = 0; index < 500; index++) {
                    results.add(lifetime.publish(new CompletableFuture<Integer>(), (data, failure) -> calls.incrementAndGet()));
                }
                return results;
            });
            ownership.detach(lifetime);
            pending.get(5, TimeUnit.SECONDS).forEach(SerialChunkOwnershipTest::assertRetired);
            loop.drain();
            assertThat(calls).hasValue(0);
        }
    }

    @Test
    void retirementClassificationDoesNotHideOtherCancellationsOrIoErrors() {
        assertThat(RetiredChunkException.isRetirement(new CompletionException(new RetiredChunkException()))).isTrue();
        assertThat(RetiredChunkException.isRetirement(new java.util.concurrent.CancellationException())).isFalse();
        assertThat(RetiredChunkException.isRetirement(new CompletionException(new IOException()))).isFalse();
        var retired = CompletableFuture.failedFuture(new RetiredChunkException());
        assertRetired(retired);
        assertThatThrownBy(() -> retired.get(5, TimeUnit.SECONDS)).satisfies(failure ->
                assertThat(RetiredChunkException.isRetirement(failure)).isTrue());
        var cancelled = new CompletableFuture<>();
        cancelled.cancel(false);
        assertThatThrownBy(cancelled::join).satisfies(failure ->
                assertThat(RetiredChunkException.isRetirement(failure)).isFalse());
    }

    private static void assertRetired(CompletableFuture<?> result) {
        assertThatThrownBy(result::join).satisfies(failure ->
                assertThat(RetiredChunkException.isRetirement(failure)).isTrue());
    }

    private static final class QueuedExecutor implements Executor {
        private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();

        @Override
        public void execute(Runnable command) {
            queue.add(command);
        }

        private void drain() {
            Runnable action;
            while ((action = queue.poll()) != null) {
                action.run();
            }
        }
    }
}
