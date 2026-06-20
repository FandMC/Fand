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
}
