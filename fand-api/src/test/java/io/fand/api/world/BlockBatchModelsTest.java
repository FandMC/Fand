package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.block.BlockType;
import io.fand.api.component.DataComponentMap;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class BlockBatchModelsTest {

    private static final BlockType STONE = new TestBlockType(Key.key("minecraft:stone"));
    private static final BlockType AIR = new TestBlockType(Key.key("minecraft:air"));

    @Test
    void blockChangeDefaultsNullComponentsToEmptyMap() {
        var change = BlockBatchChange.of(1, 2, 3, STONE, null);

        assertThat(change.components()).isEqualTo(DataComponentMap.EMPTY);
        assertThat(change.offset(10, 20, 30)).isEqualTo(BlockBatchChange.of(11, 22, 33, STONE));
        assertThatThrownBy(() -> BlockBatchChange.of(Integer.MAX_VALUE, 0, 0, STONE).offset(1, 0, 0))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void blockOptionsValidatePositiveSliceSize() {
        assertThat(BlockBatchOptions.defaults().maxBlocksPerTick())
                .isEqualTo(BlockBatchOptions.DEFAULT_MAX_BLOCKS_PER_TICK);
        assertThat(BlockBatchOptions.withoutNeighborUpdates().updateMode()).isEqualTo(BlockUpdateMode.CLIENTS_ONLY);
        assertThat(BlockBatchOptions.immediate().maxBlocksPerTick()).isEqualTo(Integer.MAX_VALUE);
        assertThatThrownBy(() -> new BlockBatchOptions(0, BlockUpdateMode.NORMAL, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockResultRejectsImpossibleCounters() {
        assertThat(BlockBatchResult.empty()).isEqualTo(new BlockBatchResult(0, 0, 0, 0));
        assertThatThrownBy(() -> new BlockBatchResult(1, 1, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BlockBatchResult(Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockRegionBuildsInclusiveAreas() {
        var world = new TestWorld();

        assertThat(BlockRegion.between(world.at(2, 5, -1), world.at(0, 3, 4)))
                .isEqualTo(new BlockRegion(0, 3, -1, 2, 5, 4));
        assertThat(BlockRegion.cube(world.at(10, 64, 10), 2))
                .isEqualTo(new BlockRegion(8, 62, 8, 12, 66, 12));
        assertThatThrownBy(() -> new BlockRegion(1, 0, 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockScanOptionsValidateBudgets() {
        assertThat(BlockScanOptions.defaults().maxBlocksPerTick())
                .isEqualTo(BlockScanOptions.DEFAULT_MAX_BLOCKS_PER_TICK);
        assertThat(BlockScanResult.empty()).isEqualTo(new BlockScanResult(0L, 0L, 0L, 0L, 0L));
        assertThatThrownBy(() -> new BlockScanOptions(0, 1, true, BlockBatchOptions.defaults()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BlockScanOptions(1, 0, true, BlockBatchOptions.defaults()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fluidBatchOptionsValidateBudgets() {
        assertThat(FluidBatchOptions.defaults().maxBlocksPerTick())
                .isEqualTo(FluidBatchOptions.DEFAULT_MAX_BLOCKS_PER_TICK);
        assertThat(FluidBatchOptions.defaults().loadedChunksOnly()).isTrue();
        assertThat(FluidBatchOptions.defaults().batchOptions().updateMode()).isEqualTo(BlockUpdateMode.CLIENTS_ONLY);
        assertThatThrownBy(() -> new FluidBatchOptions(0, 1, true, BlockBatchOptions.defaults()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FluidBatchOptions(1, 0, true, BlockBatchOptions.defaults()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clipboardKeepsRelativeBlocksWithinDeclaredSize() {
        var clipboard = BlockClipboard.of(2, 1, 2, List.of(
                BlockBatchChange.of(0, 0, 0, STONE),
                BlockBatchChange.of(1, 0, 1, AIR)));

        assertThat(clipboard.blocks()).hasSize(2);
        assertThat(clipboard.empty()).isFalse();
        assertThatThrownBy(() -> BlockClipboard.of(1, 1, 1, List.of(BlockBatchChange.of(1, 0, 0, STONE))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private record TestBlockType(Key key) implements BlockType {
    }

    private static final class TestWorld implements World {

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
    }
}
