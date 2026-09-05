package net.minecraft.world.ticks;

import io.fand.server.tick.OwnershipCell;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CellScheduledTicksTest {
    private static final Object TYPE = new Object();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void overdueTicksKeepVanillaPriorityOrderAndShareTheWorldBudget(boolean indexed) {
        var ticks = ticks(indexed);
        var left = attach(ticks, -64);
        var right = attach(ticks, 128);
        var first = tick(-64, 1, TickPriority.LOW, 0);
        var second = tick(128, 8, TickPriority.HIGH, 1);
        var third = tick(-63, 9, TickPriority.NORMAL, 2);
        left.schedule(first);
        left.schedule(third);
        right.schedule(second);
        var fired = new ArrayList<BlockPos>();

        ticks.tick(10, 2, (pos, type) -> fired.add(pos));

        assertThat(fired).containsExactly(second.pos(), first.pos());
        assertThat(ticks.cellTickCounts()).containsExactlyEntriesOf(Map.of(new OwnershipCell(-1, 0), 1));
        ticks.tick(11, 2, (pos, type) -> fired.add(pos));
        assertThat(fired).containsExactly(second.pos(), first.pos(), third.pos());
        assertThat(ticks.cellTickCounts()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void clearingCollectedTicksInvalidatesQueriesAndSaveReservations(boolean indexed) {
        var ticks = ticks(indexed);
        var first = attach(ticks, 0);
        var second = attach(ticks, 128);
        first.schedule(tick(0, 1, TickPriority.NORMAL, 0));
        second.schedule(tick(128, 1, TickPriority.NORMAL, 1));
        var fired = new ArrayList<BlockPos>();

        ticks.tick(1, 20, (pos, type) -> {
            fired.add(pos);
            assertThat(ticks.willTickThisTick(new BlockPos(128, 64, 0), TYPE)).isTrue();
            assertThat(second.pack(1)).hasSize(1);
            ticks.clearArea(area(128));
            assertThat(ticks.willTickThisTick(new BlockPos(128, 64, 0), TYPE)).isFalse();
            assertThat(second.pack(1)).isEmpty();
        });

        assertThat(fired).containsExactly(new BlockPos(0, 64, 0));
        assertThat(ticks.cellTickCounts()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void callbackAdditionsWaitForNextPassEvenWhenAlreadyDue(boolean indexed) {
        var ticks = ticks(indexed);
        var container = attach(ticks, 0);
        container.schedule(tick(0, 1, TickPriority.NORMAL, 0));
        var fired = new ArrayList<BlockPos>();
        var added = tick(1, 1, TickPriority.HIGH, 1);

        ticks.tick(1, 20, (pos, type) -> {
            fired.add(pos);
            ticks.schedule(added);
            assertThat(ticks.willTickThisTick(added.pos(), TYPE)).isFalse();
        });
        assertThat(fired).containsExactly(new BlockPos(0, 64, 0));
        ticks.tick(2, 20, (pos, type) -> fired.add(pos));
        assertThat(fired).containsExactly(new BlockPos(0, 64, 0), added.pos());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void replacingAContainerDetachesOldCallbacksAndPendingExecution(boolean indexed) {
        var ticks = ticks(indexed);
        var trigger = attach(ticks, 0);
        var old = attach(ticks, 128);
        var replacement = new LevelChunkTicks<Object>();
        trigger.schedule(tick(0, 1, TickPriority.NORMAL, 0));
        old.schedule(tick(128, 1, TickPriority.NORMAL, 1));
        replacement.schedule(tick(129, 3, TickPriority.NORMAL, 2));
        var fired = new ArrayList<BlockPos>();

        ticks.tick(1, 20, (pos, type) -> {
            fired.add(pos);
            // Vanilla saves before unregistering the old chunk's tick containers.
            assertThat(old.pack(1)).hasSize(1);
            ticks.addContainer(new ChunkPos(8, 0), replacement);
            ticks.removeContainer(new ChunkPos(8, 0), old);
            assertThat(ticks.willTickThisTick(new BlockPos(128, 64, 0), TYPE)).isFalse();
            assertThat(old.count()).isEqualTo(1);
            old.schedule(tick(130, 0, TickPriority.HIGH, 3));
        });

        assertThat(fired).containsExactly(new BlockPos(0, 64, 0));
        ticks.tick(2, 20, (pos, type) -> fired.add(pos));
        assertThat(fired).hasSize(1);
        ticks.tick(3, 20, (pos, type) -> fired.add(pos));
        assertThat(fired).containsExactly(new BlockPos(0, 64, 0), new BlockPos(129, 64, 0));
        assertThat(old.count()).isEqualTo(2);
        assertThat(ticks.count()).isZero();
        ticks.removeContainer(new ChunkPos(8, 0), replacement);
        assertThat(ticks.cellTickCounts()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failureReturnsUnexecutedReservationsAndReleasesThePass(boolean indexed) {
        var ticks = ticks(indexed);
        var container = attach(ticks, 0);
        container.schedule(tick(0, 1, TickPriority.NORMAL, 0));
        container.schedule(tick(1, 1, TickPriority.NORMAL, 1));
        var failure = new IllegalStateException("tick failed");

        assertThatThrownBy(() -> ticks.tick(1, 20, (pos, type) -> {
            assertThatThrownBy(() -> ticks.tick(1, 20, (nestedPos, nestedType) -> {}))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("reentered");
            throw failure;
        })).isSameAs(failure);

        assertThat(ticks.willTickThisTick(new BlockPos(1, 64, 0), TYPE)).isFalse();
        assertThat(container.pack(1)).extracting(SavedTick::pos).containsExactly(new BlockPos(1, 64, 0));
        var fired = new ArrayList<BlockPos>();
        ticks.tick(2, 20, (pos, type) -> fired.add(pos));
        assertThat(fired).containsExactly(new BlockPos(1, 64, 0));
        assertThat(container.pack(2)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void copyingDuringExecutionIncludesRunReservedAndFutureTicks(boolean indexed) {
        var ticks = ticks(indexed);
        var source = attach(ticks, 0);
        attach(ticks, 128);
        source.schedule(tick(0, 1, TickPriority.NORMAL, 0));
        source.schedule(tick(1, 1, TickPriority.NORMAL, 1));
        source.schedule(tick(2, 10, TickPriority.HIGH, 2));
        var fired = new ArrayList<BlockPos>();

        ticks.tick(1, 20, (pos, type) -> {
            fired.add(pos);
            if (pos.getX() == 0) {
                ticks.copyArea(new BoundingBox(0, 0, 0, 2, 255, 0), new Vec3i(128, 0, 0));
            }
        });

        assertThat(fired).containsExactly(new BlockPos(0, 64, 0), new BlockPos(1, 64, 0));
        ticks.tick(2, 20, (pos, type) -> fired.add(pos));
        assertThat(fired).containsExactly(new BlockPos(0, 64, 0), new BlockPos(1, 64, 0),
            new BlockPos(128, 64, 0), new BlockPos(129, 64, 0));
        ticks.tick(10, 20, (pos, type) -> fired.add(pos));
        assertThat(fired.subList(4, 6)).containsExactly(new BlockPos(2, 64, 0), new BlockPos(130, 64, 0));
    }

    @Test
    void deadlineIndexCanToggleWithBlockedChunksAndNeverAccumulatesOldLifetimes() {
        var indexed = new AtomicBoolean(true);
        var eligible = new AtomicBoolean(false);
        var ticks = new LevelTicks<Object>(chunk -> eligible.get(), indexed::get, CellScheduledTicksTest::cell);
        var pos = new ChunkPos(0, 0);
        for (int i = 0; i < 2000; i++) {
            var container = new LevelChunkTicks<Object>();
            container.schedule(tick(0, 10, TickPriority.NORMAL, i));
            ticks.addContainer(pos, container);
            if ((i & 1) == 0) {
                ticks.removeContainer(pos);
            }
        }
        var fired = new ArrayList<BlockPos>();
        ticks.tick(10, 20, (blockPos, type) -> fired.add(blockPos));
        indexed.set(false);
        ticks.tick(11, 20, (blockPos, type) -> fired.add(blockPos));
        assertThat(fired).isEmpty();
        indexed.set(true);
        eligible.set(true);
        ticks.tick(12, 20, (blockPos, type) -> fired.add(blockPos));
        assertThat(fired).containsExactly(new BlockPos(0, 64, 0));
        ticks.removeContainer(pos);
        assertThat(ticks.cellTickCounts()).isEmpty();
    }

    @Test
    void saveReservationsRoundTripWithoutDuplicatingANewlyScheduledTick() {
        var container = new LevelChunkTicks<Object>();
        var first = tick(0, 10, TickPriority.NORMAL, 0);
        var second = tick(1, 11, TickPriority.NORMAL, 1);
        container.schedule(first);
        container.schedule(second);
        assertThat(container.reserveForTick()).isSameAs(first);
        assertThat(container.pack(10)).extracting(SavedTick::pos).containsExactly(first.pos(), second.pos());
        container.schedule(tick(0, 20, TickPriority.HIGH, 2));

        var saved = container.pack(10);

        assertThat(saved).hasSize(2);
        assertThat(saved).filteredOn(tick -> tick.pos().equals(first.pos())).extracting(SavedTick::delay).containsExactly(10);
        var reloaded = new LevelChunkTicks<>(saved);
        reloaded.unpack(100);
        assertThat(reloaded.count()).isEqualTo(2);
        assertThat(reloaded.poll().triggerTick()).isEqualTo(101);
        assertThat(reloaded.poll().triggerTick()).isEqualTo(110);
        assertThat(reloaded.count()).isZero();
        container.restoreTick(first);
        assertThat(container.count()).isEqualTo(2);
    }

    @Test
    void randomizedSchedulingMatchesASimpleChunkQueueModelAcrossCells() {
        var indexed = new AtomicBoolean(true);
        var ticks = new LevelTicks<Object>(chunk -> true, indexed::get, CellScheduledTicksTest::cell);
        Map<Long, List<ScheduledTick<Object>>> model = new HashMap<>();
        Object[] types = {new Object(), new Object(), new Object()};
        Random random = new Random(750309);
        long now = 0;
        long order = 0;
        for (int step = 0; step < 1000; step++) {
            int chunkX = (random.nextInt(16) - 8) * 4;
            long chunk = ChunkPos.pack(chunkX, 0);
            var pos = new BlockPos(chunkX * 16 + random.nextInt(4), 64, 0);
            if (!model.containsKey(chunk)) {
                ticks.addContainer(new ChunkPos(chunkX, 0), new LevelChunkTicks<>());
                model.put(chunk, new ArrayList<>());
            }
            switch (random.nextInt(5)) {
                case 0, 1 -> {
                    Object type = types[random.nextInt(types.length)];
                    var tick = new ScheduledTick<>(type, pos, now + random.nextInt(10),
                        TickPriority.values()[random.nextInt(TickPriority.values().length)], order++);
                    ticks.schedule(tick);
                    if (model.get(chunk).stream().noneMatch(old -> old.type() == type && old.pos().equals(pos))) {
                        model.get(chunk).add(tick);
                        model.get(chunk).sort(ScheduledTick.DRAIN_ORDER);
                    }
                }
                case 2 -> {
                    ticks.clearArea(area(pos.getX()));
                    model.get(chunk).removeIf(tick -> tick.pos().equals(pos));
                }
                case 3 -> {
                    ticks.removeContainer(new ChunkPos(chunkX, 0));
                    model.remove(chunk);
                }
                default -> {
                    now += random.nextInt(3);
                    int budget = random.nextInt(12);
                    indexed.set(random.nextBoolean());
                    var expected = new ArrayList<Fired>();
                    for (int i = 0; i < budget; i++) {
                        ScheduledTick<Object> best = null;
                        List<ScheduledTick<Object>> bestQueue = null;
                        for (var queue : model.values()) {
                            if (!queue.isEmpty() && queue.getFirst().triggerTick() <= now
                                && (best == null || ScheduledTick.INTRA_TICK_DRAIN_ORDER.compare(queue.getFirst(), best) < 0)) {
                                best = queue.getFirst();
                                bestQueue = queue;
                            }
                        }
                        if (best == null) {
                            break;
                        }
                        bestQueue.removeFirst();
                        expected.add(new Fired(best.pos(), best.type()));
                    }
                    var actual = new ArrayList<Fired>();
                    ticks.tick(now, budget, (blockPos, type) -> actual.add(new Fired(blockPos, type)));
                    assertThat(actual).as("step %s", step).containsExactlyElementsOf(expected);
                }
            }
            assertThat(ticks.count()).as("count at step %s", step)
                .isEqualTo(model.values().stream().mapToInt(List::size).sum());
        }
    }

    private record Fired(BlockPos pos, Object type) {}

    private static LevelTicks<Object> ticks(boolean indexed) {
        return new LevelTicks<>(chunk -> true, () -> indexed, CellScheduledTicksTest::cell);
    }

    private static OwnershipCell cell(long chunk) {
        return new OwnershipCell(Math.floorDiv(ChunkPos.getX(chunk), 4), Math.floorDiv(ChunkPos.getZ(chunk), 4));
    }

    private static LevelChunkTicks<Object> attach(LevelTicks<Object> ticks, int blockX) {
        var container = new LevelChunkTicks<Object>();
        ticks.addContainer(new ChunkPos(Math.floorDiv(blockX, 16), 0), container);
        return container;
    }

    private static ScheduledTick<Object> tick(int x, long time, TickPriority priority, long order) {
        return new ScheduledTick<>(TYPE, new BlockPos(x, 64, 0), time, priority, order);
    }

    private static BoundingBox area(int x) {
        return new BoundingBox(x, 0, 0, x, 255, 0);
    }
}
