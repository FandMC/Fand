package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegionScheduledBlockTickerTest {
    private static final BlockPos LEFT = new BlockPos(8, 64, 8);
    private static final BlockPos RIGHT = new BlockPos(264, 64, 8);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void nativeLampCallbacksOverlapUnderTheirScheduledQueueOwnership() {
        try (Fixture fixture = new Fixture()) {
            CountDownLatch entered = new CountDownLatch(2);
            AtomicInteger changes = new AtomicInteger();
            var lit = Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true);
            doAnswer(call -> {
                awaitBoth(entered);
                BlockPos pos = call.getArgument(0);
                RegionTickScope.current().requirePosition(fixture.base.level, pos);
                assertThat(fixture.ticks.willTickThisTick(pos, Blocks.REDSTONE_LAMP)).isFalse();
                changes.incrementAndGet();
                return true;
            }).when(fixture.base.level).setBlock(any(), any(), anyInt());
            fixture.schedule(LEFT, 0);
            fixture.schedule(RIGHT, 1);
            fixture.ticks.tick(1, 2, (pos, block) -> lit.tick(fixture.base.level, pos,
                RegionTickScope.current().random(fixture.base.level)), fixture.runner::tick);
            assertThat(changes).hasValue(2);
            assertThat(fixture.runner.snapshot().peakConcurrentRegions()).isEqualTo(2);
            assertThat(fixture.left.pack(1)).isEmpty();
            assertThat(fixture.right.pack(1)).isEmpty();
        }
    }

    @Test
    void aFailedRegionReturnsOnlyItsUnstartedTicksAfterOtherRegionsFinish() {
        try (Fixture fixture = new Fixture()) {
            CountDownLatch entered = new CountDownLatch(2);
            AtomicInteger completed = new AtomicInteger();
            fixture.schedule(LEFT, 0);
            fixture.schedule(LEFT.east(), 1);
            fixture.schedule(RIGHT, 2);
            assertThatThrownBy(() -> fixture.ticks.tick(1, 3, (pos, block) -> {
                awaitBoth(entered);
                if (pos.equals(LEFT)) throw new IllegalStateException("lamp failed");
                completed.incrementAndGet();
            }, fixture.runner::tick)).isInstanceOf(CompletionException.class).hasRootCauseMessage("lamp failed");
            assertThat(completed).hasValue(1);
            assertThat(fixture.left.pack(1)).extracting(net.minecraft.world.ticks.SavedTick::pos).containsExactly(LEFT.east());
            assertThat(fixture.right.pack(1)).isEmpty();
            List<BlockPos> retried = new ArrayList<>();
            fixture.ticks.tick(2, 3, (pos, block) -> retried.add(pos));
            assertThat(retried).containsExactly(LEFT.east());
        }
    }

    @Test
    void ownedClearAndCopyUpdateTheLiveBatchWithoutCrossRegionAccess() {
        try (Fixture fixture = new Fixture()) {
            fixture.schedule(LEFT, 0);
            fixture.schedule(LEFT.east(), 1);
            fixture.schedule(RIGHT, 2);
            var next = LEFT.east(2);
            AtomicInteger callbacks = new AtomicInteger();
            fixture.ticks.tick(1, 3, (pos, block) -> {
                callbacks.incrementAndGet();
                if (pos.equals(LEFT)) {
                    assertThat(fixture.ticks.willTickThisTick(LEFT.east(), block)).isTrue();
                    fixture.ticks.copyArea(new BoundingBox(LEFT), new Vec3i(2, 0, 0));
                    assertThat(fixture.ticks.hasScheduledTick(next, block)).isTrue();
                    assertThat(fixture.ticks.willTickThisTick(next, block)).isFalse();
                    fixture.ticks.clearArea(new BoundingBox(LEFT.east()));
                    assertThat(fixture.ticks.willTickThisTick(LEFT.east(), block)).isFalse();
                    assertThatThrownBy(() -> fixture.ticks.clearArea(new BoundingBox(RIGHT)))
                        .isInstanceOf(IllegalStateException.class);
                    assertThatThrownBy(() -> fixture.ticks.copyArea(new BoundingBox(LEFT), new Vec3i(256, 0, 0)))
                        .isInstanceOf(IllegalStateException.class);
                    assertThatThrownBy(() -> fixture.ticks.willTickThisTick(RIGHT, block))
                        .isInstanceOf(IllegalStateException.class);
                    assertThatThrownBy(() -> fixture.ticks.removeContainer(new ChunkPos(LEFT.getX() >> 4, LEFT.getZ() >> 4)))
                        .isInstanceOf(IllegalStateException.class);
                    assertThatThrownBy(fixture.ticks::count).isInstanceOf(IllegalStateException.class);
                }
            }, fixture.runner::tick);
            assertThat(callbacks).hasValue(2);
            assertThat(fixture.left.pack(1)).extracting(net.minecraft.world.ticks.SavedTick::pos).containsExactly(next);
            assertThat(fixture.right.pack(1)).isEmpty();
        }
    }

    @Test
    void selectionKeepsTheWorldBudgetBeforePartitioningAndNewDueTicksWait() {
        try (Fixture fixture = new Fixture()) {
            fixture.schedule(LEFT, 0);
            fixture.schedule(RIGHT, 1);
            fixture.schedule(LEFT.east(), 2);
            AtomicInteger callbacks = new AtomicInteger();
            fixture.ticks.tick(1, 2, (pos, block) -> {
                callbacks.incrementAndGet();
                fixture.ticks.schedule(new ScheduledTick<>(block, pos, 1, 5));
            }, fixture.runner::tick);
            assertThat(callbacks).hasValue(2);
            assertThat(fixture.ticks.count()).isEqualTo(3);
            assertThat(fixture.runner.snapshot().completedTickers()).isEqualTo(2);
        }
    }

    @Test
    void mixedAndUnauditedRedstoneGroupsRemainOnControl() {
        try (Fixture fixture = new Fixture()) {
            fixture.schedule(LEFT, 0);
            fixture.ticks.schedule(new ScheduledTick<>(Blocks.REPEATER, LEFT.east(), 1, 1));
            fixture.schedule(RIGHT, 2);
            List<Block> called = new ArrayList<>();
            Thread control = Thread.currentThread();
            fixture.ticks.tick(1, 3, (pos, block) -> {
                assertThat(Thread.currentThread()).isSameAs(control);
                assertThat(RegionTickScope.current()).isNull();
                called.add(block);
            }, fixture.runner::tick);
            assertThat(called).containsExactly(Blocks.REDSTONE_LAMP, Blocks.REPEATER, Blocks.REDSTONE_LAMP);
            assertThat(RegionScheduledBlockTicker.supported(Blocks.OBSERVER)).isFalse();
            assertThat(RegionScheduledBlockTicker.supported(Blocks.COMPARATOR)).isFalse();
            assertThat(RegionScheduledBlockTicker.supported(Blocks.REDSTONE_TORCH)).isFalse();
        }
    }

    @Test
    void aDispatchTokenCanOnlyClaimItsReservationOnce() {
        try (Fixture fixture = new Fixture()) {
            fixture.schedule(LEFT, 0);
            AtomicInteger calls = new AtomicInteger();
            fixture.ticks.tick(1, 1, (pos, block) -> calls.incrementAndGet(), (batch, execute) -> {
                execute.accept(batch.getFirst());
                execute.accept(batch.getFirst());
            });
            assertThat(calls).hasValue(1);
            assertThat(fixture.left.pack(1)).isEmpty();
        }
    }

    private static void awaitBoth(CountDownLatch entered) {
        entered.countDown();
        try {
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final RegionBlockEntityTickerTest.Fixture base = new RegionBlockEntityTickerTest.Fixture(2);
        final LevelTicks<Block> ticks = new LevelTicks<>(chunk -> true, () -> true,
            chunk -> base.topology.cellAtChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)));
        final LevelChunkTicks<Block> left = new LevelChunkTicks<>();
        final LevelChunkTicks<Block> right = new LevelChunkTicks<>();
        final RegionScheduledBlockTicker runner = new RegionScheduledBlockTicker(base.level, () -> base.workers);

        Fixture() {
            ticks.addContainer(new ChunkPos(LEFT.getX() >> 4, LEFT.getZ() >> 4), left);
            ticks.addContainer(new ChunkPos(RIGHT.getX() >> 4, RIGHT.getZ() >> 4), right);
            when(base.level.getBlockTicks()).thenReturn(ticks);
        }

        void schedule(BlockPos pos, long order) {
            ticks.schedule(new ScheduledTick<>(Blocks.REDSTONE_LAMP, pos, 1, TickPriority.NORMAL, order));
        }

        @Override public void close() { base.close(); }
    }
}
