package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

final class ChunkGenerationTaskTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void statusGraphDoesNotWaitForWholeLayerBeforeRunningReadyDependency() {
        ChunkPos center = new ChunkPos(0, 0);
        ChunkPos blocked = new ChunkPos(-8, -8);
        var chunkMap = new GraphTestChunkMap(blocked);
        ChunkGenerationTask task = ChunkGenerationTask.create(chunkMap, ChunkStatus.BIOMES, center);

        CompletableFuture<?> waitFor = task.runUntilWait();

        assertThat(waitFor).isNotNull();
        assertThat(chunkMap.holder(center).startedStatuses()).contains(ChunkStatus.STRUCTURE_STARTS);
        assertThat(chunkMap.holder(center).startedStatuses()).doesNotContain(ChunkStatus.BIOMES);

        chunkMap.completeBlockedEmpty();

        assertThat(waitFor).isDone();
        assertThat(chunkMap.holder(center).startedStatuses()).contains(ChunkStatus.BIOMES);
        assertThat(task.runUntilWait()).isNull();
    }

    @Test
    void statusGraphFailsTargetFutureWhenDependencyFails() {
        ChunkPos center = new ChunkPos(0, 0);
        ChunkPos blocked = new ChunkPos(-8, -8);
        var chunkMap = new GraphTestChunkMap(blocked);
        ChunkGenerationTask task = ChunkGenerationTask.create(chunkMap, ChunkStatus.BIOMES, center);

        CompletableFuture<?> waitFor = task.runUntilWait();
        assertThat(waitFor).isNotNull();

        chunkMap.failBlockedEmpty();

        assertThat(waitFor).isDone();
        assertThat(chunkMap.holder(center).fand$getStatusFutureForTesting(ChunkStatus.BIOMES)).succeedsWithin(java.time.Duration.ofSeconds(1))
            .satisfies(result -> assertThat(result.isSuccess()).isFalse());
        assertThat(task.runUntilWait()).isNull();
    }

    @Test
    void holderDoesNotSerializeIndependentStatusNodesByStartedCursor() {
        ChunkPos center = new ChunkPos(0, 0);
        var chunkMap = new GraphTestChunkMap(new ChunkPos(99, 99));
        TestChunkHolder holder = chunkMap.holderOrCreate(center);

        GenerationChunkHolder.StatusGenerationClaim startsClaim = holder.claimGenerationStatus(ChunkStatus.STRUCTURE_STARTS);
        GenerationChunkHolder.StatusGenerationClaim referencesClaim = holder.claimGenerationStatus(ChunkStatus.STRUCTURE_REFERENCES);

        assertThat(startsClaim.owner()).isTrue();
        assertThat(referencesClaim.owner()).isTrue();

        CompletableFuture<ChunkResult<ChunkAccess>> starts = holder.applyStep(
            ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.STRUCTURE_STARTS), chunkMap, chunkMap.cache(center, 8)
        );
        CompletableFuture<ChunkResult<ChunkAccess>> references = holder.applyStep(
            ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.STRUCTURE_REFERENCES), chunkMap, chunkMap.cache(center, 8)
        );

        assertThat(starts).isDone();
        assertThat(references).isDone();
        assertThat(chunkMap.holder(center).startedStatuses()).contains(
            ChunkStatus.STRUCTURE_STARTS,
            ChunkStatus.STRUCTURE_REFERENCES
        );
    }

    private static final class GraphTestChunkMap implements GeneratingChunkMap {
        private final Map<Long, TestChunkHolder> holders = new ConcurrentHashMap<>();
        private final ChunkPos blockedEmptyPos;
        private final CompletableFuture<ChunkResult<ChunkAccess>> blockedEmpty = new CompletableFuture<>();

        private GraphTestChunkMap(final ChunkPos blockedEmptyPos) {
            this.blockedEmptyPos = blockedEmptyPos;
        }

        @Override
        public GenerationChunkHolder acquireGeneration(final long chunkNode) {
            TestChunkHolder holder = this.holderOrCreate(ChunkPos.unpack(chunkNode));
            holder.increaseGenerationRefCount();
            return holder;
        }

        @Override
        public void releaseGeneration(final GenerationChunkHolder chunkHolder) {
            chunkHolder.decreaseGenerationRefCount();
        }

        @Override
        public CompletableFuture<ChunkAccess> applyStep(
            final GenerationChunkHolder chunkHolder, final ChunkStep step, final StaticCache2D<GenerationChunkHolder> cache
        ) {
            TestChunkHolder holder = (TestChunkHolder)chunkHolder;
            holder.startedStatuses().add(step.targetStatus());
            CompletableFuture<ChunkResult<ChunkAccess>> result = this.runStep(holder, step);
            return result.thenApply(chunkResult -> {
                if (!chunkResult.isSuccess()) {
                    throw new IllegalStateException(chunkResult.getError());
                }

                holder.persistedStatus = step.targetStatus();
                return chunkResult.orElse(null);
            });
        }

        @Override
        public ChunkGenerationTask scheduleGenerationTask(final ChunkStatus targetStatus, final ChunkPos pos) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void runGenerationTasks() {
        }

        private TestChunkHolder holder(final ChunkPos pos) {
            return this.holders.get(pos.pack());
        }

        private TestChunkHolder holderOrCreate(final ChunkPos pos) {
            return this.holders.computeIfAbsent(pos.pack(), key -> new TestChunkHolder(ChunkPos.unpack(key), this));
        }

        private StaticCache2D<GenerationChunkHolder> cache(final ChunkPos center, final int radius) {
            return StaticCache2D.createLazy(center.x(), center.z(), radius, (x, z) -> this.holderOrCreate(new ChunkPos(x, z)));
        }

        private CompletableFuture<ChunkResult<ChunkAccess>> runStep(final TestChunkHolder holder, final ChunkStep step) {
            if (step.targetStatus() == ChunkStatus.EMPTY && holder.getPos().equals(this.blockedEmptyPos)) {
                return this.blockedEmpty;
            }

            return CompletableFuture.completedFuture(ChunkResult.of(chunk(holder.getPos(), step.targetStatus())));
        }

        private void completeBlockedEmpty() {
            this.blockedEmpty.complete(ChunkResult.of(chunk(this.blockedEmptyPos, ChunkStatus.EMPTY)));
        }

        private void failBlockedEmpty() {
            this.blockedEmpty.complete(ChunkResult.error("blocked"));
        }
    }

    private static final class TestChunkHolder extends GenerationChunkHolder {
        private final GraphTestChunkMap chunkMap;
        private final List<ChunkStatus> startedStatuses = new ArrayList<>();
        private @Nullable ChunkStatus persistedStatus;

        private TestChunkHolder(final ChunkPos pos, final GraphTestChunkMap chunkMap) {
            super(pos);
            this.chunkMap = chunkMap;
            this.updateHighestAllowedStatus(null);
        }

        private List<ChunkStatus> startedStatuses() {
            return this.startedStatuses;
        }

        @Override
        public @Nullable ChunkStatus getPersistedStatus() {
            return this.persistedStatus;
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

    private static ChunkAccess chunk(final ChunkPos pos, final ChunkStatus status) {
        ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, LevelHeightAccessor.create(0, 0), null, null);
        chunk.setPersistedStatus(status);
        return chunk;
    }
}
