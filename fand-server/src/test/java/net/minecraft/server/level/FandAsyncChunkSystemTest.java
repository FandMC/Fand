package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

final class FandAsyncChunkSystemTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void submitsSameLaneTasksTogether() {
        try (var lanes = new RegionizedChunkTaskScheduler(2, "test"); var dispatcher = new RecordingDispatcher()) {
            var system = new FandAsyncChunkSystem(null, dispatcher, lanes);
            long firstChunk = ChunkPos.pack(0, 0);
            long sameLaneChunk = ChunkPos.pack(7, 7);
            assertThat(lanes.laneIndex(sameLaneChunk)).isEqualTo(lanes.laneIndex(firstChunk));

            var firstWait = new CompletableFuture<>();
            var first = new TestTask(firstChunk, firstWait);
            var sameLane = new TestTask(sameLaneChunk, (CompletableFuture<?>)null);
            system.enqueueReadyTaskForTesting(first, firstChunk);
            system.enqueueReadyTaskForTesting(sameLane, sameLaneChunk);

            system.runGenerationTasks();

            assertThat(dispatcher.submittedPositions()).containsExactly(firstChunk, sameLaneChunk);
        }
    }

    @Test
    void submitsDifferentLanesTogether() {
        try (var lanes = new RegionizedChunkTaskScheduler(2, "test"); var dispatcher = new RecordingDispatcher()) {
            var system = new FandAsyncChunkSystem(null, dispatcher, lanes);
            long firstChunk = ChunkPos.pack(0, 0);
            long otherLaneChunk = ChunkPos.pack(RegionizedChunkTaskScheduler.REGION_SIZE_CHUNKS, 0);
            assertThat(lanes.laneIndex(otherLaneChunk)).isNotEqualTo(lanes.laneIndex(firstChunk));

            system.enqueueReadyTaskForTesting(new TestTask(firstChunk, new CompletableFuture<>()), firstChunk);
            system.enqueueReadyTaskForTesting(new TestTask(otherLaneChunk, new CompletableFuture<>()), otherLaneChunk);

            system.runGenerationTasks();

            assertThat(dispatcher.submittedPositions()).containsExactly(firstChunk, otherLaneChunk);
        }
    }

    @Test
    void coalescesNestedDrainRequestsWithoutDroppingReadyTasks() {
        try (var lanes = new RegionizedChunkTaskScheduler(2, "test"); var dispatcher = new RecordingDispatcher()) {
            var system = new FandAsyncChunkSystem(null, dispatcher, lanes);
            long firstChunk = ChunkPos.pack(0, 0);
            long nestedChunk = ChunkPos.pack(RegionizedChunkTaskScheduler.REGION_SIZE_CHUNKS, 0);

            system.enqueueReadyTaskForTesting(new TestTask(firstChunk, null, () -> {
                    system.enqueueReadyTaskForTesting(new TestTask(nestedChunk, (CompletableFuture<?>)null), nestedChunk);
                    system.runGenerationTasks();
            }), firstChunk);

            system.runGenerationTasks();

            assertThat(dispatcher.submittedPositions()).containsExactly(firstChunk, nestedChunk);
        }
    }

    @Test
    void drainsHigherPriorityReadyTasksFirst() {
        try (var lanes = new RegionizedChunkTaskScheduler(1, "test"); var dispatcher = new RecordingDispatcher()) {
            var system = new FandAsyncChunkSystem(null, dispatcher, lanes);
            long lowPriorityChunk = ChunkPos.pack(0, 0);
            long highPriorityChunk = ChunkPos.pack(1, 0);

            system.enqueueReadyTaskForTesting(new TestTask(lowPriorityChunk, (CompletableFuture<?>)null, 20), lowPriorityChunk);
            system.enqueueReadyTaskForTesting(new TestTask(highPriorityChunk, (CompletableFuture<?>)null, 1), highPriorityChunk);

            system.runGenerationTasks();

            assertThat(dispatcher.submittedPositions()).containsExactly(highPriorityChunk, lowPriorityChunk);
        }
    }

    @Test
    void drainsTaskQueuedByWaitCompletionAfterDispatchFinishes() {
        try (var lanes = new RegionizedChunkTaskScheduler(1, "test"); var dispatcher = new HoldingDispatcher()) {
            var system = new FandAsyncChunkSystem(null, dispatcher, lanes);
            long chunk = ChunkPos.pack(0, 0);
            List<@Nullable CompletableFuture<?>> waits = new ArrayList<>();
            waits.add(new CompleteOnRegistrationFuture());
            waits.add(null);
            var task = new TestTask(chunk, waits);

            system.enqueueReadyTaskForTesting(task, chunk);
            system.runGenerationTasks();

            assertThat(dispatcher.submittedPositions()).containsExactly(chunk);

            dispatcher.runNext();

            assertThat(dispatcher.submittedPositions()).containsExactly(chunk, chunk);
        }
    }

    private static final class TestTask implements FandAsyncChunkSystem.TaskHandle {
        private final ChunkPos pos;
        private final GenerationChunkHolder center;
        private final FandAsyncTaskState state = new FandAsyncTaskState();
        private final List<@Nullable CompletableFuture<?>> waitFor;
        private final Runnable beforeWait;
        private int runs;

        private TestTask(final long chunkPos, final @Nullable CompletableFuture<?> waitFor) {
            this(chunkPos, nullableList(waitFor), 1);
        }

        private TestTask(final long chunkPos, final List<@Nullable CompletableFuture<?>> waitFor) {
            this(chunkPos, waitFor, 1);
        }

        private TestTask(final long chunkPos, final @Nullable CompletableFuture<?> waitFor, final Runnable beforeWait) {
            this(chunkPos, nullableList(waitFor), 1, beforeWait);
        }

        private TestTask(final long chunkPos, final @Nullable CompletableFuture<?> waitFor, final int queueLevel) {
            this(chunkPos, nullableList(waitFor), queueLevel, () -> {
            });
        }

        private TestTask(final long chunkPos, final List<@Nullable CompletableFuture<?>> waitFor, final int queueLevel) {
            this(chunkPos, waitFor, queueLevel, () -> {
            });
        }

        private TestTask(
            final long chunkPos,
            final List<@Nullable CompletableFuture<?>> waitFor,
            final int queueLevel,
            final Runnable beforeWait
        ) {
            this.pos = ChunkPos.unpack(chunkPos);
            this.center = new TestChunkHolder(this.pos, queueLevel);
            this.waitFor = waitFor;
            this.beforeWait = beforeWait;
        }

        @Override
        public ChunkPos pos() {
            return this.pos;
        }

        @Override
        public GenerationChunkHolder center() {
            return this.center;
        }

        @Override
        public @Nullable CompletableFuture<?> runUntilWait() {
            this.beforeWait.run();
            int index = this.runs++;
            return index < this.waitFor.size() ? this.waitFor.get(index) : null;
        }

        @Override
        public boolean markQueued() {
            return this.state.markQueued();
        }

        @Override
        public boolean markDispatchStarted() {
            return this.state.markDispatchStarted();
        }

        @Override
        public boolean markDispatchFinished() {
            return this.state.markDispatchFinished();
        }

        private static List<@Nullable CompletableFuture<?>> nullableList(final @Nullable CompletableFuture<?> waitFor) {
            List<@Nullable CompletableFuture<?>> waits = new ArrayList<>(1);
            waits.add(waitFor);
            return waits;
        }
    }

    private static final class TestChunkHolder extends GenerationChunkHolder {
        private final int queueLevel;

        private TestChunkHolder(final ChunkPos pos, final int queueLevel) {
            super(pos);
            this.queueLevel = queueLevel;
        }

        @Override
        protected void addSaveDependency(final CompletableFuture<?> sync) {
        }

        @Override
        public int getTicketLevel() {
            return 1;
        }

        @Override
        public int getQueueLevel() {
            return this.queueLevel;
        }
    }

    private static final class RecordingDispatcher extends ChunkTaskDispatcher {
        private final List<Runnable> submittedTasks = new ArrayList<>();
        private final List<Long> submittedPositions = new ArrayList<>();

        private RecordingDispatcher() {
            super(TaskScheduler.wrapExecutor("test-worldgen", Runnable::run), Runnable::run);
        }

        @Override
        public void submit(final Runnable task, final long pos, final java.util.function.IntSupplier level) {
            this.submittedTasks.add(task);
            this.submittedPositions.add(pos);
            task.run();
        }

        private List<Long> submittedPositions() {
            return this.submittedPositions;
        }
    }

    private static final class HoldingDispatcher extends ChunkTaskDispatcher {
        private final ArrayDeque<Runnable> submittedTasks = new ArrayDeque<>();
        private final List<Long> submittedPositions = new ArrayList<>();

        private HoldingDispatcher() {
            super(TaskScheduler.wrapExecutor("test-worldgen", Runnable::run), Runnable::run);
        }

        @Override
        public void submit(final Runnable task, final long pos, final java.util.function.IntSupplier level) {
            this.submittedTasks.add(task);
            this.submittedPositions.add(pos);
        }

        private void runNext() {
            this.submittedTasks.removeFirst().run();
        }

        private List<Long> submittedPositions() {
            return this.submittedPositions;
        }
    }

    private static final class CompleteOnRegistrationFuture extends CompletableFuture<Void> {
        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public CompletableFuture<Void> whenComplete(final BiConsumer<? super Void, ? super Throwable> action) {
            super.complete(null);
            return super.whenComplete(action);
        }
    }
}
