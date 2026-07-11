package net.minecraft.world.ticks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

final class LevelTicksTest {

    @Test
    void queuedContainersRetryBlockedChunksOnNextTick() {
        AtomicBoolean canTickChunk = new AtomicBoolean(false);
        AtomicInteger tickChecks = new AtomicInteger();
        LevelTicks<Object> ticks = new LevelTicks<>(chunk -> {
            int checks = tickChecks.incrementAndGet();
            if (checks > 2) {
                throw new AssertionError("queued tick container was retried in a tight loop");
            }
            return canTickChunk.get();
        }, () -> true);
        LevelChunkTicks<Object> container = new LevelChunkTicks<>();
        Object type = new Object();
        BlockPos pos = new BlockPos(1, 64, 1);
        container.schedule(new ScheduledTick<>(type, pos, 10L, 0L));
        ticks.addContainer(new ChunkPos(0, 0), container);

        List<Object> fired = new ArrayList<>();
        ticks.tick(10L, 16, (tickPos, tickType) -> fired.add(tickType));

        assertThat(fired).isEmpty();
        assertThat(ticks.hasScheduledTick(pos, type)).isTrue();
        assertThat(tickChecks).hasValue(1);

        canTickChunk.set(true);
        ticks.tick(11L, 16, (tickPos, tickType) -> fired.add(tickType));

        assertThat(fired).containsExactly(type);
        assertThat(ticks.hasScheduledTick(pos, type)).isFalse();
        assertThat(tickChecks).hasValue(2);
    }

    @Test
    void chunkTicksDrainByTimePriorityAndSubTickOrder() {
        LevelChunkTicks<Object> container = new LevelChunkTicks<>();
        Object typeA = new Object();
        Object typeB = new Object();
        Object typeC = new Object();
        ScheduledTick<Object> late = new ScheduledTick<>(typeA, new BlockPos(1, 64, 1), 12L, TickPriority.NORMAL, 0L);
        ScheduledTick<Object> high = new ScheduledTick<>(typeB, new BlockPos(2, 64, 1), 10L, TickPriority.HIGH, 2L);
        ScheduledTick<Object> first = new ScheduledTick<>(typeC, new BlockPos(3, 64, 1), 10L, TickPriority.HIGH, 1L);

        container.schedule(late);
        container.schedule(high);
        container.schedule(first);

        assertThat(container.poll()).isSameAs(first);
        assertThat(container.poll()).isSameAs(high);
        assertThat(container.poll()).isSameAs(late);
        assertThat(container.poll()).isNull();
    }

    @Test
    void chunkTicksDeduplicateByPositionAndType() {
        LevelChunkTicks<Object> container = new LevelChunkTicks<>();
        Object type = new Object();
        BlockPos pos = new BlockPos(1, 64, 1);
        ScheduledTick<Object> first = new ScheduledTick<>(type, pos, 10L, 0L);
        ScheduledTick<Object> duplicate = new ScheduledTick<>(type, pos, 11L, 1L);

        container.schedule(first);
        container.schedule(duplicate);

        assertThat(container.count()).isEqualTo(1);
        assertThat(container.poll()).isSameAs(first);
        assertThat(container.hasScheduledTick(pos, type)).isFalse();
    }

    @Test
    void chunkTicksRemoveIfUpdatesMembershipAndHead() {
        LevelChunkTicks<Object> container = new LevelChunkTicks<>();
        Object typeA = new Object();
        Object typeB = new Object();
        ScheduledTick<Object> first = new ScheduledTick<>(typeA, new BlockPos(1, 64, 1), 10L, 0L);
        ScheduledTick<Object> second = new ScheduledTick<>(typeB, new BlockPos(2, 64, 1), 11L, 1L);

        container.schedule(first);
        container.schedule(second);
        container.removeIf(tick -> tick.type() == typeA);

        assertThat(container.hasScheduledTick(first.pos(), typeA)).isFalse();
        assertThat(container.peek()).isSameAs(second);
        assertThat(container.count()).isEqualTo(1);
    }

    @Test
    void pendingChunkTicksUnpackIntoScheduledQueue() {
        Object type = new Object();
        SavedTick<Object> pending = new SavedTick<>(type, new BlockPos(1, 64, 1), 5, TickPriority.NORMAL);
        LevelChunkTicks<Object> container = new LevelChunkTicks<>(List.of(pending));

        assertThat(container.count()).isEqualTo(1);
        assertThat(container.peek()).isNull();

        container.unpack(20L);

        assertThat(container.peek().triggerTick()).isEqualTo(25L);
        assertThat(container.poll().type()).isSameAs(type);
        assertThat(container.count()).isZero();
    }
}
