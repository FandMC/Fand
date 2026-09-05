package net.minecraft.world.ticks;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.server.tick.OwnershipCell;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class ConcurrentCellSchedulingTest {
    @Test
    void independentCellsPublishDeadlinesWithoutLosingTicks() throws Exception {
        LevelTicks<String> ticks = new LevelTicks<>(chunk -> true, () -> true,
            chunk -> new OwnershipCell(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)));
        ticks.addContainer(new ChunkPos(0, 0), new LevelChunkTicks<>());
        ticks.addContainer(new ChunkPos(4, 0), new LevelChunkTicks<>());
        CountDownLatch ready = new CountDownLatch(2);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var left = workers.submit(() -> schedule(ticks, 0, ready));
            var right = workers.submit(() -> schedule(ticks, 64, ready));
            left.get(10, TimeUnit.SECONDS);
            right.get(10, TimeUnit.SECONDS);
        }
        assertThat(ticks.count()).isEqualTo(4000);
        AtomicInteger executed = new AtomicInteger();
        ticks.tick(5000, 4000, (pos, type) -> executed.incrementAndGet());
        assertThat(executed).hasValue(4000);
        assertThat(ticks.cellTickCounts()).isEmpty();
    }

    private static void schedule(LevelTicks<String> ticks, int baseX, CountDownLatch ready) {
        ready.countDown();
        try {
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
        for (int i = 0; i < 2000; i++) {
            BlockPos pos = new BlockPos(baseX + (i & 15), i >> 8, i >> 4 & 15);
            ticks.schedule(new ScheduledTick<>("tick", pos, 2000 - i, i));
        }
    }
}
