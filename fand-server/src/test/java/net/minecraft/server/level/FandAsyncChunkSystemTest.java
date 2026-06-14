package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    void keepsSameLaneTaskQueuedWhileLaneIsRunning() {
        try (var lanes = new RegionizedChunkTaskScheduler(2, "test"); var dispatcher = new RecordingDispatcher()) {
            var system = new FandAsyncChunkSystem(null, dispatcher, lanes);
            long firstChunk = ChunkPos.pack(0, 0);
            long sameLaneChunk = ChunkPos.pack(7, 7);
            assertThat(lanes.laneIndex(sameLaneChunk)).isEqualTo(lanes.laneIndex(firstChunk));

            var firstWait = new CompletableFuture<>();
            var first = new TestTask(firstChunk, firstWait);
            var sameLane = new TestTask(sameLaneChunk, null);
            system.enqueueReadyTaskForTesting(first, firstChunk);
            system.enqueueReadyTaskForTesting(sameLane, sameLaneChunk);

            system.runGenerationTasks();

            assertThat(dispatcher.submittedPositions()).containsExactly(firstChunk);

            dispatcher.submittedTasks().get(0).run();

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

    private static final class TestTask implements FandAsyncChunkSystem.TaskHandle {
        private final ChunkPos pos;
        private final GenerationChunkHolder center;
        private final @Nullable CompletableFuture<?> waitFor;

        private TestTask(final long chunkPos, final @Nullable CompletableFuture<?> waitFor) {
            this.pos = ChunkPos.unpack(chunkPos);
            this.center = new TestChunkHolder(this.pos);
            this.waitFor = waitFor;
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
            return this.waitFor;
        }
    }

    private static final class TestChunkHolder extends GenerationChunkHolder {
        private TestChunkHolder(final ChunkPos pos) {
            super(pos);
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
            return 1;
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
        }

        private List<Runnable> submittedTasks() {
            return this.submittedTasks;
        }

        private List<Long> submittedPositions() {
            return this.submittedPositions;
        }
    }
}
