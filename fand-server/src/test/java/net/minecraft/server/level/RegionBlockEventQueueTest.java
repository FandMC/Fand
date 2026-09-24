package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

import io.fand.server.tick.OwnershipCell;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.BlockEventQueue;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegionBlockEventQueueTest {
    private static final BlockPos LEFT = new BlockPos(8, 64, 8);
    private static final BlockPos RIGHT = new BlockPos(264, 64, 8);

    @BeforeAll
    static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void nativeRegionCallbacksCanConcurrentlyEnqueueAndClearTheirOwnEvents() {
        try (Fixture fixture = new Fixture()) {
            CountDownLatch entered = new CountDownLatch(2);
            fixture.base.runner.tick(List.of(
                ticker(LEFT, () -> produce(fixture, LEFT, entered)),
                ticker(RIGHT, () -> produce(fixture, RIGHT, entered))), TickingBlockEntity::tick);
            assertThat(fixture.events.cellCounts()).containsOnlyKeys(new OwnershipCell(0, 0), new OwnershipCell(4, 0));
            List<BlockEventData> fired = new ArrayList<>();
            fixture.events.run(pos -> true, fired::add);
            assertThat(fired).containsExactlyInAnyOrder(event(LEFT, 2000), event(RIGHT, 2000));
            assertThat(fixture.events.cellCounts()).isEmpty();
        }
    }

    private static void produce(Fixture fixture, BlockPos pos, CountDownLatch entered) {
        entered.countDown();
        try {
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
        for (int id = 0; id < 2000; id++) {
            fixture.base.level.blockEvent(pos, Blocks.NOTE_BLOCK, id, 0);
            fixture.base.level.clearBlockEvents(new BoundingBox(pos));
        }
        fixture.base.level.blockEvent(pos, Blocks.NOTE_BLOCK, 2000, 0);
        fixture.base.level.blockEvent(pos, Blocks.NOTE_BLOCK, 2000, 0);
    }

    @Test
    void invalidWorldRangeAndLifecycleCallsCannotChangePendingEvents() {
        try (Fixture fixture = new Fixture()) {
            fixture.events.add(event(LEFT, 1));
            fixture.events.add(event(RIGHT, 2));
            var otherWorld = new BlockEventQueue(mock(ServerLevel.class), pos -> new OwnershipCell(0, 0));
            fixture.base.runner.tick(List.of(ticker(LEFT, () -> {
                assertThatThrownBy(() -> fixture.base.level.blockEvent(RIGHT, Blocks.NOTE_BLOCK, 3, 0))
                    .isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> otherWorld.add(event(LEFT, 4))).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> fixture.base.level.clearBlockEvents(
                    new BoundingBox(LEFT.getX(), 64, 8, RIGHT.getX(), 64, 8))).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> fixture.events.run(pos -> true, event -> {})).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> fixture.events.removeChunk(new ChunkPos(0, 0))).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> fixture.events.removeIf(event -> true)).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(fixture.events::cellCounts).isInstanceOf(IllegalStateException.class);
            }), ticker(RIGHT, () -> {})), TickingBlockEntity::tick);
            List<BlockEventData> fired = new ArrayList<>();
            fixture.events.run(pos -> true, fired::add);
            assertThat(fired).containsExactly(event(LEFT, 1), event(RIGHT, 2));
            assertThat(otherWorld.cellCounts()).isEmpty();
        }
    }

    @Test
    void ownedEndpointsDoNotPermitClearingAnUnownedHoleInsideTheRange() {
        try (Fixture fixture = new Fixture()) {
            fixture.base.topology.tryActivate(new OwnershipCell(0, 2));
            fixture.base.topology.tryActivate(new OwnershipCell(2, 3));
            fixture.base.topology.tryActivate(new OwnershipCell(4, 2));
            fixture.base.topology.tryActivate(new OwnershipCell(10, 0));
            assertThat(fixture.base.topology.ownerOf(new OwnershipCell(0, 0)))
                .isEqualTo(fixture.base.topology.ownerOf(new OwnershipCell(4, 0)));
            assertThat(fixture.base.topology.ownerOf(new OwnershipCell(2, 0))).isEmpty();
            fixture.events.add(event(LEFT, 1));
            fixture.events.add(event(RIGHT, 2));
            fixture.base.runner.tick(List.of(ticker(LEFT, () -> {
                RegionTickScope.current().requirePosition(fixture.base.level, RIGHT);
                assertThatThrownBy(() -> fixture.base.level.clearBlockEvents(
                    new BoundingBox(LEFT.getX(), 64, 8, RIGHT.getX(), 64, 8))).isInstanceOf(IllegalStateException.class);
            }), ticker(new BlockPos(648, 64, 8), () -> {})), TickingBlockEntity::tick);
            List<BlockEventData> fired = new ArrayList<>();
            fixture.events.run(pos -> true, fired::add);
            assertThat(fired).containsExactly(event(LEFT, 1), event(RIGHT, 2));
        }
    }

    private static BlockEventData event(BlockPos pos, int id) {
        return new BlockEventData(pos, Blocks.NOTE_BLOCK, id, 0);
    }

    private static TickingBlockEntity ticker(BlockPos pos, Runnable action) {
        return new TickingBlockEntity() {
            @Override public void tick() { action.run(); }
            @Override public boolean isRemoved() { return false; }
            @Override public BlockPos getPos() { return pos; }
            @Override public String getType() { return "test:block_events"; }
            @Override public boolean fand$canTickInRegion() { return true; }
        };
    }

    private static final class Fixture implements AutoCloseable {
        final RegionBlockEntityTickerTest.Fixture base = new RegionBlockEntityTickerTest.Fixture(2);
        final BlockEventQueue events = new BlockEventQueue(this.base.level,
            pos -> this.base.topology.cellAtChunk(pos.getX() >> 4, pos.getZ() >> 4));

        Fixture() {
            try {
                var field = ServerLevel.class.getDeclaredField("blockEvents");
                field.setAccessible(true);
                field.set(this.base.level, this.events);
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(failure);
            }
            doCallRealMethod().when(this.base.level).blockEvent(any(), any(), anyInt(), anyInt());
            doCallRealMethod().when(this.base.level).clearBlockEvents(any());
        }

        @Override public void close() { this.base.close(); }
    }
}
