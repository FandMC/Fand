package net.minecraft.world.level;

import io.fand.server.tick.OwnershipCell;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlockEventQueueTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void interleavedCellsPreserveOrderAndDeduplicatePendingEvents() {
        var queue = queue();
        var first = event(128, 1);
        var second = event(-1, 2);
        var third = event(129, 3);
        queue.add(first);
        queue.add(second);
        queue.add(third);
        queue.add(event(128, 1));
        var fired = new ArrayList<BlockEventData>();

        queue.run(pos -> true, fired::add);

        assertThat(fired).containsExactly(first, second, third);
        assertThat(queue.cellCounts()).isEmpty();
    }

    @Test
    void eventsAddedByAnEventStillRunInTheSamePass() {
        var queue = queue();
        var first = event(-1, 1);
        var second = event(128, 2);
        queue.add(first);
        var fired = new ArrayList<BlockEventData>();

        queue.run(pos -> true, current -> {
            fired.add(current);
            if (fired.size() == 1) {
                queue.add(second);
                queue.add(first);
            }
        });

        assertThat(fired).containsExactly(first, second, first);
    }

    @Test
    void inactiveCellsAreDeferredOnceAndResumeInOrder() {
        var queue = queue();
        var first = event(-1, 1);
        var second = event(128, 2);
        queue.add(first);
        queue.add(second);
        var checks = new AtomicInteger();
        var fired = new ArrayList<BlockEventData>();

        queue.run(pos -> { checks.incrementAndGet(); return false; }, fired::add);

        assertThat(fired).isEmpty();
        assertThat(checks).hasValue(2);
        assertThat(queue.cellCounts()).containsExactlyInAnyOrderEntriesOf(
            Map.of(new OwnershipCell(-1, 0), 1, new OwnershipCell(2, 0), 1));
        queue.run(pos -> true, fired::add);
        assertThat(fired).containsExactly(first, second);
    }

    @Test
    void areaRemovalIncludesEventsAlreadyDeferredByThisPass() {
        var queue = queue();
        queue.add(event(-1, 1));
        var live = event(128, 2);
        queue.add(live);
        var fired = new ArrayList<BlockEventData>();

        queue.run(pos -> pos.getX() >= 0, event -> {
            fired.add(event);
            queue.removeIf(pending -> pending.pos().getX() < 0);
        });
        queue.run(pos -> true, fired::add);

        assertThat(fired).containsExactly(live);
        assertThat(queue.cellCounts()).isEmpty();
    }

    @Test
    void unloadingAChunkKeepsOtherChunksInTheSameCell() {
        var queue = queue();
        queue.add(event(64, 1));
        var neighbor = event(80, 2);
        var distant = event(128, 3);
        queue.add(neighbor);
        queue.add(distant);
        queue.removeChunk(new ChunkPos(4, 0));
        var fired = new ArrayList<BlockEventData>();

        queue.run(pos -> true, fired::add);

        assertThat(fired).containsExactly(neighbor, distant);
        assertThat(queue.cellCounts()).isEmpty();
    }

    @Test
    void unloadingDuringAnEventCannotResurrectDeferredEvents() {
        var queue = queue();
        queue.add(event(-1, 1));
        var live = event(128, 2);
        queue.add(live);
        queue.run(pos -> pos.getX() >= 0, event -> queue.removeChunk(new ChunkPos(-1, 0)));

        assertThat(queue.cellCounts()).isEmpty();
    }

    @Test
    void failureKeepsUnprocessedAndDeferredEventsAndReleasesThePass() {
        var queue = queue();
        var deferred = event(-1, 1);
        var failing = event(128, 2);
        var last = event(129, 3);
        queue.add(deferred);
        queue.add(failing);
        queue.add(last);
        var failure = new IllegalStateException("event failed");

        assertThatThrownBy(() -> queue.run(pos -> pos.getX() >= 0, event -> {
            assertThatThrownBy(() -> queue.run(pos -> true, ignored -> {}))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("reentered");
            throw failure;
        })).isSameAs(failure);
        var fired = new ArrayList<BlockEventData>();
        queue.run(pos -> true, fired::add);

        assertThat(fired).containsExactly(last, deferred);
    }

    @Test
    void mutablePositionsCannotMoveAnEventOrCorruptItsDeduplicationKey() {
        var queue = queue();
        var position = new BlockPos.MutableBlockPos(64, 64, 0);
        var event = new BlockEventData(position, Blocks.NOTE_BLOCK, 1, 0);
        queue.add(event);
        position.set(128, 64, 0);
        queue.add(event(64, 1));
        var fired = new ArrayList<BlockEventData>();
        queue.run(pos -> true, fired::add);

        assertThat(fired).containsExactly(event(64, 1));
        assertThat(queue.cellCounts()).isEmpty();
    }

    @Test
    void cellDiagnosticsAreDetachedAndReadOnly() {
        var queue = queue();
        queue.add(event(-1, 1));
        var snapshot = queue.cellCounts();
        queue.removeChunk(new ChunkPos(-1, 0));

        assertThat(snapshot).containsExactlyEntriesOf(Map.of(new OwnershipCell(-1, 0), 1));
        assertThatThrownBy(snapshot::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThat(queue.cellCounts()).isEmpty();
    }

    private static BlockEventQueue queue() {
        return new BlockEventQueue(pos -> new OwnershipCell(Math.floorDiv(pos.getX(), 64), Math.floorDiv(pos.getZ(), 64)));
    }

    private static BlockEventData event(int x, int id) {
        return new BlockEventData(new BlockPos(x, 64, 0), Blocks.NOTE_BLOCK, id, 0);
    }
}
