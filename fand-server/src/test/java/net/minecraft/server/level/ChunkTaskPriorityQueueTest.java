package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ChunkTaskPriorityQueueTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void popSkipsExcludedChunkPositions() {
        var queue = new ChunkTaskPriorityQueue("test");
        long firstChunk = net.minecraft.world.level.ChunkPos.pack(0, 0);
        long secondChunk = net.minecraft.world.level.ChunkPos.pack(1, 0);
        var excluded = new LongOpenHashSet();

        queue.submit(() -> {}, firstChunk, 1);
        queue.submit(() -> {}, secondChunk, 1);
        excluded.add(firstChunk);

        var second = queue.pop(excluded);

        assertThat(second).isNotNull();
        assertThat(second.chunkPos()).isEqualTo(secondChunk);
        assertThat(queue.pop(excluded)).isNull();

        excluded.clear();
        var first = queue.pop(excluded);

        assertThat(first).isNotNull();
        assertThat(first.chunkPos()).isEqualTo(firstChunk);
    }

    @Test
    void popChoosesHighestPriorityForSameChunk() {
        var queue = new ChunkTaskPriorityQueue("test");
        long chunk = net.minecraft.world.level.ChunkPos.pack(0, 0);

        queue.submit(() -> {}, chunk, 2);
        queue.submit(() -> {}, chunk, 1);

        var priorityOne = queue.pop();
        assertThat(priorityOne).isNotNull();
        assertThat(priorityOne.chunkPos()).isEqualTo(chunk);
        assertThat(priorityOne.tasks()).hasSize(1);

        var priorityTwo = queue.pop();
        assertThat(priorityTwo).isNotNull();
        assertThat(priorityTwo.chunkPos()).isEqualTo(chunk);
        assertThat(priorityTwo.tasks()).hasSize(1);

        assertThat(queue.pop()).isNull();
    }

    @Test
    void popAvoidingSkipsOverlappingWorldgenWriteEnvelopes() {
        var queue = new ChunkTaskPriorityQueue("test");
        long activeChunk = net.minecraft.world.level.ChunkPos.pack(0, 0);
        long distanceOne = net.minecraft.world.level.ChunkPos.pack(1, 0);
        long distanceFour = net.minecraft.world.level.ChunkPos.pack(4, 4);
        long distanceFive = net.minecraft.world.level.ChunkPos.pack(5, 0);

        queue.submit(() -> {}, distanceOne, 1);
        queue.submit(() -> {}, distanceFour, 1);
        queue.submit(() -> {}, distanceFive, 1);

        var farEnough = queue.popAvoiding(candidate -> ParallelChunkTaskDispatcher.hasOverlappingWriteEnvelope(activeChunk, candidate));

        assertThat(farEnough).isNotNull();
        assertThat(farEnough.chunkPos()).isEqualTo(distanceFive);
        assertThat(queue.popAvoiding(candidate -> ParallelChunkTaskDispatcher.hasOverlappingWriteEnvelope(activeChunk, candidate))).isNull();
    }

    @Test
    void parallelWorldgenWriteEnvelopesOnlyOverlapWithinFourChunks() {
        long center = net.minecraft.world.level.ChunkPos.pack(0, 0);

        assertThat(ParallelChunkTaskDispatcher.hasOverlappingWriteEnvelope(center, net.minecraft.world.level.ChunkPos.pack(0, 0))).isTrue();
        assertThat(ParallelChunkTaskDispatcher.hasOverlappingWriteEnvelope(center, net.minecraft.world.level.ChunkPos.pack(2, 2))).isTrue();
        assertThat(ParallelChunkTaskDispatcher.hasOverlappingWriteEnvelope(center, net.minecraft.world.level.ChunkPos.pack(4, 4))).isTrue();
        assertThat(ParallelChunkTaskDispatcher.hasOverlappingWriteEnvelope(center, net.minecraft.world.level.ChunkPos.pack(5, 0))).isFalse();
        assertThat(ParallelChunkTaskDispatcher.hasOverlappingWriteEnvelope(center, net.minecraft.world.level.ChunkPos.pack(0, 5))).isFalse();
    }

    @Test
    void writeEnvelopeTrackerKeepsSharedCoverageUntilEveryOwnerCompletes() {
        var tracker = new ParallelChunkTaskDispatcher.WriteEnvelopeTracker();
        long firstActive = net.minecraft.world.level.ChunkPos.pack(0, 0);
        long secondActive = net.minecraft.world.level.ChunkPos.pack(5, 0);
        long sharedCandidate = net.minecraft.world.level.ChunkPos.pack(3, 0);
        long firstOnlyCandidate = net.minecraft.world.level.ChunkPos.pack(-4, 0);
        long secondOnlyCandidate = net.minecraft.world.level.ChunkPos.pack(9, 0);

        assertThat(ParallelChunkTaskDispatcher.hasOverlappingWriteEnvelope(firstActive, secondActive)).isFalse();

        tracker.add(firstActive);
        tracker.add(secondActive);

        assertThat(tracker.contains(sharedCandidate)).isTrue();
        assertThat(tracker.contains(firstOnlyCandidate)).isTrue();
        assertThat(tracker.contains(secondOnlyCandidate)).isTrue();

        tracker.remove(firstActive);

        assertThat(tracker.contains(sharedCandidate)).isTrue();
        assertThat(tracker.contains(firstOnlyCandidate)).isFalse();
        assertThat(tracker.contains(secondOnlyCandidate)).isTrue();

        tracker.remove(secondActive);

        assertThat(tracker.contains(sharedCandidate)).isFalse();
        assertThat(tracker.contains(secondOnlyCandidate)).isFalse();
    }
}
