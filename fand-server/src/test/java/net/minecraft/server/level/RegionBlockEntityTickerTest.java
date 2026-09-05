package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fand.server.component.PersistentComponentData;
import io.fand.server.tick.OwnershipCell;
import io.fand.server.tick.RegionContext;
import io.fand.server.tick.RegionSimulationWorkers;
import io.fand.server.tick.TickRegionTopology;
import io.fand.server.util.ServerThreading;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ThreadUnsafeRandom;
import net.minecraft.world.level.block.entity.BlockEntityTickList;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegionBlockEntityTickerTest {
    private static final BlockPos LEFT = new BlockPos(8, 64, 8);
    private static final BlockPos RIGHT = new BlockPos(264, 64, 8);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void nativeCallbacksOverlapAndKeepTheOrderWithinEachRegion() {
        try (Fixture fixture = new Fixture(2)) {
            CountDownLatch entered = new CountDownLatch(2);
            List<Integer> leftOrder = new ArrayList<>();
            List<Integer> rightOrder = new ArrayList<>();
            List<TickingBlockEntity> tickers = List.of(
                ticker(LEFT, () -> { awaitBoth(entered); leftOrder.add(1); }),
                ticker(RIGHT, () -> { awaitBoth(entered); rightOrder.add(1); }),
                ticker(LEFT, () -> leftOrder.add(2)), ticker(RIGHT, () -> rightOrder.add(2)));
            fixture.runner.tick(tickers, ticker -> {
                assertThat(RegionContext.current()).isPresent();
                RegionTickScope.current().requirePosition(fixture.level, ticker.getPos());
                ticker.tick();
            });
            assertThat(leftOrder).containsExactly(1, 2);
            assertThat(rightOrder).containsExactly(1, 2);
            var stats = fixture.runner.snapshot();
            assertThat(stats.completedPhases()).isEqualTo(1);
            assertThat(stats.completedTickers()).isEqualTo(4);
            assertThat(stats.peakConcurrentRegions()).isEqualTo(2);
            assertThat(stats.lastExecutions()).extracting(RegionBlockEntityTicker.Execution::thread).doesNotHaveDuplicates();
            assertThat(RegionTickScope.current()).isNull();
            assertThat(RegionContext.current()).isEmpty();
        }
    }

    @Test
    void publicationsRunOnControlAfterEveryRegionEvenWhenOneFails() {
        try (Fixture fixture = new Fixture(2)) {
            CountDownLatch entered = new CountDownLatch(2);
            AtomicInteger finished = new AtomicInteger();
            List<String> published = new ArrayList<>();
            Thread control = Thread.currentThread();
            Runnable publish = () -> {
                assertThat(Thread.currentThread()).isSameAs(control);
                assertThat(RegionTickScope.current()).isNull();
                assertThat(finished).hasValue(2);
                published.add("done");
            };
            var left = ticker(LEFT, () -> {
                awaitBoth(entered);
                RegionTickScope.defer(fixture.level, publish);
                finished.incrementAndGet();
                throw new IllegalArgumentException("tick failure");
            });
            var right = ticker(RIGHT, () -> {
                awaitBoth(entered);
                RegionTickScope.defer(fixture.level, publish);
                finished.incrementAndGet();
            });
            assertThatThrownBy(() -> fixture.runner.tick(List.of(left, right), TickingBlockEntity::tick))
                .isInstanceOf(CompletionException.class).hasRootCauseMessage("tick failure");
            assertThat(published).containsExactly("done", "done");
            assertThat(fixture.topology.regions()).noneMatch(region -> region.state() == io.fand.server.tick.TickRegion.State.RUNNING);
        }
    }

    @Test
    void aMixedRegionKeepsAllItsCallbacksOnControl() {
        try (Fixture fixture = new Fixture(2)) {
            List<String> calls = new ArrayList<>();
            Ticker unsafe = new Ticker(LEFT, false, () -> {
                assertThat(RegionTickScope.current()).isNull();
                calls.add("unsafe");
            });
            fixture.runner.tick(List.of(unsafe, ticker(LEFT, () -> calls.add("same region")),
                ticker(RIGHT, () -> calls.add("other region"))), TickingBlockEntity::tick);
            assertThat(calls).containsExactly("unsafe", "same region", "other region");
            assertThat(fixture.runner.snapshot().completedPhases()).isZero();
        }
    }

    @Test
    void serialCallbacksCanMergeCandidateRegionsBeforeTheyAreDispatched() {
        try (Fixture fixture = new Fixture(2)) {
            BlockPos third = new BlockPos(648, 64, 8);
            fixture.topology.tryActivate(new OwnershipCell(10, 0));
            AtomicInteger calls = new AtomicInteger();
            Ticker control = new Ticker(third, false, () -> fixture.topology.tryActivate(new OwnershipCell(2, 0)));
            Runnable check = () -> {
                assertThat(RegionTickScope.current()).isNull();
                calls.incrementAndGet();
            };
            fixture.runner.tick(List.of(ticker(LEFT, check), ticker(RIGHT, check), control), TickingBlockEntity::tick);
            assertThat(calls).hasValue(2);
            assertThat(fixture.runner.snapshot().completedPhases()).isZero();
        }
    }

    @Test
    void additionsWaitForTheNextPassAndRemovalsSuppressLaterCallbacks() {
        try (Fixture fixture = new Fixture(2)) {
            BlockEntityTickList list = new BlockEntityTickList(pos -> fixture.topology.cellAtChunk(pos.getX() >> 4, pos.getZ() >> 4));
            AtomicInteger removedCalls = new AtomicInteger();
            AtomicInteger addedCalls = new AtomicInteger();
            Ticker removed = ticker(LEFT, removedCalls::incrementAndGet);
            Ticker added = ticker(LEFT, addedCalls::incrementAndGet);
            list.add(ticker(LEFT, () -> { list.remove(removed); list.add(added); }));
            list.add(removed);
            list.add(ticker(RIGHT, () -> {}));
            list.tick(true, pos -> true, fixture.runner::tick);
            assertThat(removedCalls).hasValue(0);
            assertThat(addedCalls).hasValue(0);
            assertThat(list.pendingCellSizes()).containsEntry(new OwnershipCell(0, 0), 1);
            list.tick(true, pos -> true, fixture.runner::tick);
            assertThat(addedCalls).hasValue(1);
        }
    }

    @Test
    void scopesRejectOtherRegionsOtherWorldsAndBlockingControlCalls() {
        try (Fixture fixture = new Fixture(2)) {
            Runnable left = () -> {
                var scope = RegionTickScope.current();
                scope.requirePosition(fixture.level, LEFT);
                assertThatThrownBy(() -> scope.requirePosition(fixture.level, RIGHT)).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> scope.requirePosition(mock(ServerLevel.class), LEFT)).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> ServerThreading.callBlocking(fixture.level.getServer(), () -> "bad"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("cannot block");
                assertThat(scope.random(fixture.level)).isNotSameAs(fixture.controlRandom);
            };
            fixture.runner.tick(List.of(ticker(LEFT, left), ticker(RIGHT, () -> {})), TickingBlockEntity::tick);
        }
    }

    @Test
    void oneConfiguredWorkerPreservesTheSerialPath() {
        try (Fixture fixture = new Fixture(1)) {
            AtomicInteger calls = new AtomicInteger();
            Runnable check = () -> {
                assertThat(RegionTickScope.current()).isNull();
                calls.incrementAndGet();
            };
            fixture.runner.tick(List.of(ticker(LEFT, check), ticker(RIGHT, check)), TickingBlockEntity::tick);
            assertThat(calls).hasValue(2);
        }
    }

    @Test
    void ownedComponentWritesAreImmediatelyVisibleAndPublishAfterThePhase() {
        try (Fixture fixture = new Fixture(2)) {
            var key = net.kyori.adventure.key.Key.key("test:region");
            var leftComponents = io.fand.server.component.BlockComponentStorage.container(fixture.level, LEFT);
            var rightComponents = io.fand.server.component.BlockComponentStorage.container(fixture.level, RIGHT);
            var left = ticker(LEFT, () -> {
                leftComponents.set(key, new com.google.gson.JsonPrimitive(1));
                assertThat(leftComponents.snapshot().values().get(key).getAsInt()).isEqualTo(1);
                assertThat(fixture.components.empty(Long.toString(LEFT.asLong()))).isTrue();
                assertThatThrownBy(rightComponents::snapshot).isInstanceOf(IllegalStateException.class);
            });
            var right = ticker(RIGHT, () -> {
                rightComponents.set(key, new com.google.gson.JsonPrimitive(2));
                rightComponents.clear();
                assertThat(rightComponents.snapshot().empty()).isTrue();
                rightComponents.set(key, new com.google.gson.JsonPrimitive(3));
            });
            fixture.runner.tick(List.of(left, right), TickingBlockEntity::tick);
            assertThat(leftComponents.snapshot().values().get(key).getAsInt()).isEqualTo(1);
            assertThat(rightComponents.snapshot().values().get(key).getAsInt()).isEqualTo(3);
        }
    }

    private static Ticker ticker(BlockPos pos, Runnable action) {
        return new Ticker(pos, true, action);
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

    private record Ticker(BlockPos getPos, boolean fand$canTickInRegion, Runnable action) implements TickingBlockEntity {
        @Override public void tick() { this.action.run(); }
        @Override public boolean isRemoved() { return false; }
        @Override public String getType() { return "test:region"; }
    }

    private static final class Fixture implements AutoCloseable {
        final TickRegionTopology topology = new TickRegionTopology(4, 1);
        final ServerLevel level = mock(ServerLevel.class);
        final ThreadUnsafeRandom controlRandom = new ThreadUnsafeRandom(42);
        final PersistentComponentData components = new PersistentComponentData();
        final RegionSimulationWorkers workers;
        final RegionBlockEntityTicker runner;

        Fixture(int parallelism) {
            this.workers = new RegionSimulationWorkers(parallelism);
            this.topology.tryActivate(new OwnershipCell(0, 0));
            this.topology.tryActivate(new OwnershipCell(4, 0));
            var chunkSource = mock(ServerChunkCache.class);
            var chunkMap = mock(ChunkMap.class);
            try {
                var field = ServerChunkCache.class.getField("chunkMap");
                field.setAccessible(true);
                field.set(chunkSource, chunkMap);
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(failure);
            }
            when(this.level.getChunkSource()).thenReturn(chunkSource);
            when(chunkMap.fand$cellAtChunk(anyInt(), anyInt())).thenAnswer(call -> this.topology.cellAtChunk(call.getArgument(0), call.getArgument(1)));
            when(chunkMap.fand$regionAt(any())).thenAnswer(call -> this.topology.ownerOf(call.getArgument(0)).orElse(null));
            when(this.level.getRandom()).thenReturn(this.controlRandom);
            when(this.level.fand$newRegionNeighborUpdater()).thenAnswer(call -> new CollectingNeighborUpdater(this.level, 64));
            var storage = mock(SavedDataStorage.class);
            when(this.level.getDataStorage()).thenReturn(storage);
            when(storage.get(any())).thenReturn(this.components);
            when(storage.computeIfAbsent(any())).thenReturn(this.components);
            var server = mock(MinecraftServer.class);
            when(this.level.getServer()).thenReturn(server);
            when(server.isSameThread()).thenReturn(true);
            this.runner = new RegionBlockEntityTicker(this.level, () -> this.workers);
        }

        @Override public void close() {
            this.workers.close();
            this.topology.close();
        }
    }
}
