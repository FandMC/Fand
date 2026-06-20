package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.block.BlockType;
import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class WorldBlockBatchApiTest {

    private static final BlockType STONE = new TestBlockType(Key.key("minecraft:stone"));
    private static final BlockType AIR = new TestBlockType(Key.key("minecraft:air"));

    @Test
    void defaultSetBlocksRemainsOptInForImplementations() {
        var world = new TestWorld();

        assertThat(world.setBlocks(List.of()).join()).isEqualTo(BlockBatchResult.empty());
        assertThat(world.setBlocks(List.of(BlockBatchChange.of(0, 64, 0, STONE))).isCompletedExceptionally()).isTrue();
        assertThat(world.scanBlocks(
                        new BlockRegion(0, 0, 0, 0, 0, 0),
                        block -> null,
                        BlockScanOptions.defaults()))
                .isCompletedExceptionally();
        assertThat(world.replaceConnectedBlocks(world.at(0, 64, 0), type -> true, AIR, 8))
                .isCompletedExceptionally();
    }

    @Test
    void defaultConnectedReplaceRejectsNegativeDistance() {
        var world = new TestWorld();

        assertThat(world.replaceConnectedBlocks(world.at(0, 64, 0), type -> true, AIR, -1))
                .isCompletedExceptionally();
    }

    @Test
    void defaultFillExpandsInclusiveCuboidCoordinates() {
        var world = new TestWorld(true);

        var result = world.fillBlocks(world.at(1, 2, 3), world.at(2, 3, 4), STONE).join();

        assertThat(result).isEqualTo(new BlockBatchResult(8, 8, 0, 0));
        assertThat(world.changes).containsExactly(
                BlockBatchChange.of(1, 2, 3, STONE),
                BlockBatchChange.of(2, 2, 3, STONE),
                BlockBatchChange.of(1, 2, 4, STONE),
                BlockBatchChange.of(2, 2, 4, STONE),
                BlockBatchChange.of(1, 3, 3, STONE),
                BlockBatchChange.of(2, 3, 3, STONE),
                BlockBatchChange.of(1, 3, 4, STONE),
                BlockBatchChange.of(2, 3, 4, STONE));
    }

    @Test
    void defaultFillRejectsOversizedCuboidsBeforeAllocatingChanges() {
        var world = new TestWorld(true);

        assertThat(world.fillBlocks(world.at(0, 0, 0), world.at(Integer.MAX_VALUE, 0, 0), STONE))
                .isCompletedExceptionally();
        assertThat(world.changes).isEmpty();
    }

    @Test
    void defaultFillDoesNotMaterializeLargestRepresentableBatchBeforeSetBlocks() {
        var world = new LazyInspectingWorld();

        var result = world.fillBlocks(world.at(0, 0, 0), world.at(Integer.MAX_VALUE - 1L, 0, 0), STONE).join();

        assertThat(result).isEqualTo(new BlockBatchResult(Integer.MAX_VALUE, 1, 0, 0));
        assertThat(world.firstChange).isEqualTo(BlockBatchChange.of(0, 0, 0, STONE));
    }

    @Test
    void blockVolumeCapsBeforeLongOverflow() {
        assertThat(BlockBatchVolumes.cappedVolume(
                        Integer.MIN_VALUE,
                        0,
                        Integer.MIN_VALUE,
                        Integer.MAX_VALUE,
                        0,
                        Integer.MAX_VALUE))
                .isEqualTo(BlockBatchVolumes.TOO_MANY_BLOCKS);
    }

    @Test
    void defaultPasteOffsetsClipboardFromOrigin() {
        var world = new TestWorld(true);
        var clipboard = BlockClipboard.of(2, 1, 1, List.of(
                BlockBatchChange.of(0, 0, 0, STONE),
                BlockBatchChange.of(1, 0, 0, AIR)));

        var result = world.pasteBlocks(world.at(10, 64, -5), clipboard).join();

        assertThat(result).isEqualTo(new BlockBatchResult(2, 2, 0, 0));
        assertThat(world.changes).containsExactly(
                BlockBatchChange.of(10, 64, -5, STONE),
                BlockBatchChange.of(11, 64, -5, AIR));
    }

    private record TestBlockType(Key key) implements BlockType {
    }

    private static final class TestWorld extends BaseTestWorld {

        private final boolean supportsBatch;
        private final List<BlockBatchChange> changes = new ArrayList<>();

        private TestWorld() {
            this(false);
        }

        private TestWorld(boolean supportsBatch) {
            this.supportsBatch = supportsBatch;
        }

        @Override
        public CompletableFuture<BlockBatchResult> setBlocks(
                Collection<BlockBatchChange> changes,
                BlockBatchOptions options
        ) {
            if (!supportsBatch) {
                return defaultSetBlocks(changes, options);
            }
            this.changes.addAll(changes);
            return CompletableFuture.completedFuture(new BlockBatchResult(changes.size(), changes.size(), 0, 0));
        }
    }

    private static final class LazyInspectingWorld extends BaseTestWorld {

        private BlockBatchChange firstChange;

        @Override
        public CompletableFuture<BlockBatchResult> setBlocks(
                Collection<BlockBatchChange> changes,
                BlockBatchOptions options
        ) {
            firstChange = changes.iterator().next();
            return CompletableFuture.completedFuture(new BlockBatchResult(changes.size(), 1, 0, 0));
        }
    }

    private abstract static class BaseTestWorld implements World {

        @Override
        public Key key() {
            return Key.key("minecraft:overworld");
        }

        @Override
        public long seed() {
            return 0;
        }

        @Override
        public long gameTime() {
            return 0;
        }

        @Override
        public CompletableFuture<Void> setGameTime(long ticks) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public long time() {
            return 0;
        }

        @Override
        public CompletableFuture<Void> setTime(long ticks) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Difficulty difficulty() {
            return Difficulty.NORMAL;
        }

        @Override
        public CompletableFuture<Void> setDifficulty(Difficulty difficulty) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean storm() {
            return false;
        }

        @Override
        public CompletableFuture<Void> setStorm(boolean storm) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean thundering() {
            return false;
        }

        @Override
        public CompletableFuture<Void> setThundering(boolean thundering) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public WorldBorder worldBorder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Boolean> save() {
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public Collection<? extends Player> players() {
            return List.of();
        }

        @Override
        public Collection<? extends Entity> entities() {
            return List.of();
        }

        @Override
        public Iterable<? extends Audience> audiences() {
            return List.of();
        }

        @Override
        public void playSound(Location location, io.fand.api.world.sound.SoundEffect sound) {
        }

        @Override
        public void spawnParticle(
                Location location,
                io.fand.api.world.particle.ParticleEffect effect,
                io.fand.api.world.particle.ParticleEmission emission
        ) {
        }

        @Override
        public io.fand.api.block.Block blockAt(int x, int y, int z) {
            throw new UnsupportedOperationException();
        }

        protected CompletableFuture<BlockBatchResult> defaultSetBlocks(
                Collection<BlockBatchChange> changes,
                BlockBatchOptions options
        ) {
            return World.super.setBlocks(changes, options);
        }
    }
}
