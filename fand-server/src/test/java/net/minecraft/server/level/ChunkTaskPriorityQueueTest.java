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
    void popChunkAtPriorityRemovesOnlyRequestedPriority() {
        var queue = new ChunkTaskPriorityQueue("test");
        long chunk = net.minecraft.world.level.ChunkPos.pack(0, 0);

        queue.submit(() -> {}, chunk, 2);
        queue.submit(() -> {}, chunk, 1);

        var priorityTwo = queue.popChunkAtPriority(chunk, 2);
        assertThat(priorityTwo).isNotNull();
        assertThat(priorityTwo.tasks()).hasSize(1);

        var priorityOne = queue.popChunkAtPriority(chunk, 1);
        assertThat(priorityOne).isNotNull();
        assertThat(priorityOne.tasks()).hasSize(1);

        assertThat(queue.popChunkAtPriority(chunk, 1)).isNull();
    }

    @Test
    void popAvoidingSkipsUnavailableChunkPositions() {
        var queue = new ChunkTaskPriorityQueue("test");
        long activeChunk = net.minecraft.world.level.ChunkPos.pack(0, 0);
        long distanceOne = net.minecraft.world.level.ChunkPos.pack(1, 0);
        long distanceFive = net.minecraft.world.level.ChunkPos.pack(5, 0);

        queue.submit(() -> {}, activeChunk, 1);
        queue.submit(() -> {}, distanceOne, 1);
        queue.submit(() -> {}, distanceFive, 1);

        var next = queue.popAvoiding(candidate -> candidate == activeChunk || candidate == distanceOne);

        assertThat(next).isNotNull();
        assertThat(next.chunkPos()).isEqualTo(distanceFive);
        assertThat(queue.popAvoiding(candidate -> candidate == activeChunk || candidate == distanceOne)).isNull();
    }
}
