package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fand.server.tick.RegionContext;
import io.fand.server.tick.SerialChunkOwnership;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ChunkOwnershipIntegrationTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void retiredPublicationCompletesVanillaStatusAsUnloadedWithoutRelayingACrash() throws Exception {
        var loop = new TestEventLoop();
        try (var ownership = new SerialChunkOwnership<GenerationChunkHolder>(loop, 4, 1)) {
            var holder = new TestHolder();
            holder.fand$bindOwnership(ownership.attach(0, 0, holder));
            var prepared = new CompletableFuture<ChunkAccess>();
            var publication = holder.fand$ownership().publish(prepared, (chunk, failure) -> chunk);
            var map = mock(GeneratingChunkMap.class);
            when(map.applyStep(eq(holder), any(), any())).thenReturn(publication);
            var status = holder.applyStep(ChunkPyramid.GENERATION_PYRAMID.getStepTo(
                    net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY), map,
                    StaticCache2D.create(0, 0, 0, (x, z) -> holder));
            var crashField = BlockableEventLoop.class.getDeclaredField("delayedCrash");
            crashField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var previous = (Supplier<CrashReport>) crashField.get(null);
            int previousSuppressed = previous == null ? 0 : previous.get().getException().getSuppressed().length;

            ownership.close();

            assertThat(status).isDone();
            assertThat(status.join().isSuccess()).isFalse();
            assertThat(crashField.get(null)).isSameAs(previous);
            if (previous != null) {
                assertThat(previous.get().getException().getSuppressed()).hasSize(previousSuppressed);
            }
            assertThat(prepared).isNotDone();
        }
    }

    @Test
    void nativeHolderBindsOnceAndKeepsItsLifetimeUntilFinalDetach() {
        try (var ownership = new SerialChunkOwnership<GenerationChunkHolder>(Runnable::run, 4, 1)) {
            var holder = new TestHolder();
            var lifetime = ownership.attach(0, 0, holder);
            holder.fand$bindOwnership(lifetime);
            assertThat(holder.fand$ownership()).isSameAs(lifetime);
            assertThatThrownBy(() -> holder.fand$bindOwnership(lifetime)).isInstanceOf(IllegalStateException.class);
            assertThat(lifetime.active()).isTrue();
            ownership.detach(lifetime);
            assertThat(holder.fand$ownership().active()).isFalse();
        }
    }

    @Test
    void publicationProgressesDuringVanillaManagedBlockingAndCanAccessAnotherCell() throws Exception {
        var loop = new TestEventLoop();
        try (var ownership = new SerialChunkOwnership<String>(loop, 4, 1);
             var worker = Executors.newSingleThreadExecutor()) {
            var first = ownership.attach(0, 0, "first");
            var distant = ownership.attach(100, 0, "distant");
            var prepared = new CompletableFuture<String>();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            var publication = first.publish(prepared, (chunk, failure) -> {
                assertThat(RegionContext.current()).isEmpty();
                var nested = distant.publish(CompletableFuture.completedFuture(chunk), (other, thrown) -> other);
                loop.managedBlock(() -> nested.isDone() || System.nanoTime() >= deadline);
                assertThat(nested).isDone();
                return nested.join();
            });
            worker.submit(() -> prepared.complete("loaded")).get(5, TimeUnit.SECONDS);
            assertThat(publication).isNotDone();
            loop.managedBlock(() -> publication.isDone() || System.nanoTime() >= deadline);

            assertThat(publication).isCompletedWithValue("loaded");
            assertThat(loop.getPendingTasksCount()).isZero();
        }
    }

    private static final class TestHolder extends GenerationChunkHolder {
        private TestHolder() {
            super(new ChunkPos(0, 0));
            updateHighestAllowedStatus(null);
        }

        @Override
        protected void addSaveDependency(CompletableFuture<?> sync) {}

        @Override
        public int getTicketLevel() { return 1; }

        @Override
        public int getQueueLevel() { return 1; }
    }

    private static final class TestEventLoop extends BlockableEventLoop<Runnable> {
        private final Thread thread = Thread.currentThread();

        private TestEventLoop() {
            super("ownership-test", false);
        }

        @Override
        protected boolean shouldRun(Runnable task) { return true; }

        @Override
        protected boolean scheduleExecutables() { return true; }

        @Override
        protected Thread getRunningThread() { return thread; }

        @Override
        public Runnable wrapRunnable(Runnable task) { return task; }
    }
}
