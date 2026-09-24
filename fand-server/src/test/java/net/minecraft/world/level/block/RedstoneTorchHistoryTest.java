package net.minecraft.world.level.block;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class RedstoneTorchHistoryTest {
    private static final BlockPos LEFT = new BlockPos(8, 64, 8);
    private static final BlockPos RIGHT = new BlockPos(264, 64, 8);

    @Test
    void eighthTransitionBurnsOutAndTheSixtiethTickIsStillIncluded() {
        var history = new RedstoneTorchHistory();
        for (int i = 0; i < 7; i++) {
            assertThat(history.isBurnedOut(LEFT, 100, true)).isFalse();
        }
        assertThat(history.isBurnedOut(LEFT, 100, true)).isTrue();
        assertThat(history.isBurnedOut(LEFT, 160, false)).isTrue();
        assertThat(history.isBurnedOut(LEFT, 161, false)).isFalse();
        assertThat(history.trackedPositions()).isZero();
    }

    @Test
    void extraTransitionsDoNotExtendTheLifetimeOfOlderOnes() {
        var history = new RedstoneTorchHistory();
        for (int time = 0; time < 16; time++) {
            history.isBurnedOut(LEFT, time, true);
        }
        assertThat(history.isBurnedOut(LEFT, 68, false)).isTrue();
        assertThat(history.isBurnedOut(LEFT, 69, false)).isFalse();
        history.expire(76);
        assertThat(history.trackedPositions()).isZero();
    }

    @Test
    void positionsAndWorldsHaveIndependentHistoriesAndReadsDoNotAllocateEntries() {
        var first = new RedstoneTorchHistory();
        var second = new RedstoneTorchHistory();
        for (int i = 0; i < 8; i++) first.isBurnedOut(LEFT, 0, true);
        assertThat(first.isBurnedOut(RIGHT, 0, false)).isFalse();
        assertThat(second.isBurnedOut(LEFT, 0, false)).isFalse();
        assertThat(first.trackedPositions()).isEqualTo(1);
        assertThat(second.trackedPositions()).isZero();
    }

    @Test
    void mutablePositionsAndIdleWorldCleanupCannotRetainOrMoveHistory() {
        var history = new RedstoneTorchHistory();
        var pos = LEFT.mutable();
        for (int i = 0; i < 8; i++) history.isBurnedOut(pos, 10, true);
        pos.set(RIGHT);
        assertThat(history.isBurnedOut(LEFT, 70, false)).isTrue();
        assertThat(history.isBurnedOut(RIGHT, 70, false)).isFalse();
        history.expire(71);
        assertThat(history.trackedPositions()).isZero();
    }

    @Test
    void boundedWindowsMatchTheOriginalUnboundedWorldHistory() {
        var history = new RedstoneTorchHistory();
        var original = new ArrayList<Toggle>();
        var random = new Random(671);
        long time = 0;
        for (int i = 0; i < 20000; i++) {
            time += random.nextInt(3);
            long now = time;
            original.removeIf(toggle -> now - toggle.time > 60);
            BlockPos pos = random.nextBoolean() ? LEFT : RIGHT;
            boolean add = random.nextInt(4) != 0;
            if (add) original.add(new Toggle(pos, time));
            boolean expected = original.stream().filter(toggle -> toggle.pos.equals(pos)).count() >= 8;
            assertThat(history.isBurnedOut(pos, time, add)).isEqualTo(expected);
            if (i % 64 == 0) history.expire(time);
        }
    }

    @Test
    void concurrentUpdatesDoNotLoseTransitions() throws Exception {
        var history = new RedstoneTorchHistory();
        var ready = new CountDownLatch(8);
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 8; i++) {
                tasks.add(pool.submit(() -> {
                    ready.countDown();
                    try {
                        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                    } catch (InterruptedException failure) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(failure);
                    }
                    history.isBurnedOut(LEFT, 0, true);
                    history.isBurnedOut(RIGHT, 0, true);
                }));
            }
            for (var task : tasks) task.get(10, TimeUnit.SECONDS);
        }
        assertThat(history.isBurnedOut(LEFT, 0, false)).isTrue();
        assertThat(history.isBurnedOut(RIGHT, 0, false)).isTrue();
        history.expire(61);
        assertThat(history.trackedPositions()).isZero();
    }

    private record Toggle(BlockPos pos, long time) {}
}
