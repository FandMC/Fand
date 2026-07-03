package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.block.Block;
import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class ChunkApiModelsTest {

    @Test
    void chunkPositionsExposeBlockBounds() {
        var pos = ChunkPos.of(-2, 3);

        assertThat(pos.minBlockX()).isEqualTo(-32);
        assertThat(pos.maxBlockX()).isEqualTo(-17);
        assertThat(pos.minBlockZ()).isEqualTo(48);
        assertThat(pos.maxBlockZ()).isEqualTo(63);
        assertThat(ChunkPos.containing(-1, 16)).isEqualTo(ChunkPos.of(-1, 1));
        assertThat(pos.distanceSquared(ChunkPos.of(1, -1))).isEqualTo(25);
        assertThat(pos.chebyshevDistance(ChunkPos.of(1, -1))).isEqualTo(4);
    }

    @Test
    void chunkRegionNormalizesAndIteratesInStableOrder() {
        var region = ChunkRegion.of(2, 4, 1, 3);

        assertThat(region.minX()).isEqualTo(1);
        assertThat(region.minZ()).isEqualTo(3);
        assertThat(region.maxX()).isEqualTo(2);
        assertThat(region.maxZ()).isEqualTo(4);
        assertThat(region.chunkCount()).isEqualTo(4);
        assertThat(region.width()).isEqualTo(2);
        assertThat(region.depth()).isEqualTo(2);
        assertThat(region.center()).isEqualTo(ChunkPos.of(1, 3));
        assertThat(ChunkRegion.containingBlocks(-1, 0, 16, 31)).isEqualTo(ChunkRegion.of(-1, 0, 1, 1));
        assertThat(region.chunks()).containsExactly(
                ChunkPos.of(1, 3),
                ChunkPos.of(2, 3),
                ChunkPos.of(1, 4),
                ChunkPos.of(2, 4));
    }

    @Test
    void chunkBatchOptionsValidatePositiveBudget() {
        assertThatThrownBy(() -> new ChunkBatchOptions(0, true, false))
                .isInstanceOf(IllegalArgumentException.class);
        var options = ChunkBatchOptions.defaults()
                .withForceLoaded(true)
                .withDeduplicate(false)
                .prioritize(ChunkPos.of(1, 2), new Vector3(0.0D, 0.0D, 1.0D));

        assertThat(options.forceLoaded()).isTrue();
        assertThat(options.deduplicate()).isFalse();
        assertThat(options.order()).isEqualTo(ChunkOrder.FORWARD_FIRST);
        assertThat(options.priorityCenter()).isEqualTo(ChunkPos.of(1, 2));
        assertThat(options.priorityDirection()).isEqualTo(new Vector3(0.0D, 0.0D, 1.0D));
    }

    @Test
    void chunkBatchProgressReportsCompletionRatio() {
        var progress = new ChunkBatchProgress(10, 3, 1, 1, false, false);

        assertThat(progress.completed()).isEqualTo(5);
        assertThat(progress.remaining()).isEqualTo(5);
        assertThat(progress.ratio()).isEqualTo(0.5D);
        assertThat(new ChunkBatchProgress(0, 0, 0, 0, false, true).ratio()).isEqualTo(1.0D);
    }

    @Test
    void defaultChunkBatchOperationIsUnsupportedButObservable() {
        var world = new TestWorld();
        var operation = world.loadChunks(ChunkRegion.around(0, 0, 1));

        assertThat(operation.progress().requested()).isEqualTo(9);
        assertThat(operation.done()).isTrue();
        assertThat(operation.future()).isCompletedExceptionally();
        assertThat(operation.cancel()).isFalse();
    }

    @Test
    void defaultChunkBatchOperationDoesNotConsumeOneShotIterable() {
        var world = new TestWorld();
        var iterated = new int[1];
        Iterable<ChunkPos> chunks = () -> {
            iterated[0]++;
            return List.of(ChunkPos.of(0, 0), ChunkPos.of(1, 0)).iterator();
        };

        var operation = world.loadChunks(chunks);

        assertThat(operation.progress().requested()).isZero();
        assertThat(iterated[0]).isZero();
    }

    @Test
    void chunkConvenienceMethodsForwardToWorld() {
        var world = new RecordingWorld();
        var chunk = world.chunkAt(4, -3);

        chunk.loadAround(2);
        chunk.loadAroundPrioritized(3);
        chunk.setForceLoadedAround(1, true);

        assertThat(world.loadRegions).containsExactly(ChunkRegion.around(4, -3, 2), ChunkRegion.around(4, -3, 3));
        assertThat(world.lastLoadOptions.order()).isEqualTo(ChunkOrder.NEAREST_FIRST);
        assertThat(world.lastLoadOptions.priorityCenter()).isEqualTo(ChunkPos.of(4, -3));
        assertThat(world.lastForceRegion).isEqualTo(ChunkRegion.around(4, -3, 1));
        assertThat(world.lastForceLoaded).isTrue();
    }

    @Test
    void locationChunkHelpersExposeForwardDirection() {
        var world = new TestWorld();
        var location = world.at(-1.0D, 64.0D, 32.0D, 0.0F, 0.0F);

        assertThat(location.chunkPos()).isEqualTo(ChunkPos.of(-1, 2));
        assertThat(location.horizontalDirection()).isEqualTo(new Vector3(-0.0D, 0.0D, 1.0D));
    }

    @Test
    void loadChunksAroundLocationPrioritizesTheLocationDirection() {
        var world = new RecordingWorld();
        var location = world.at(20.0D, 64.0D, 20.0D, 0.0F, 0.0F);

        world.loadChunksAround(location, 2);

        assertThat(world.loadRegions).containsExactly(ChunkRegion.around(1, 1, 2));
        assertThat(world.lastLoadOptions.order()).isEqualTo(ChunkOrder.FORWARD_FIRST);
        assertThat(world.lastLoadOptions.priorityCenter()).isEqualTo(ChunkPos.of(1, 1));
        assertThat(world.lastLoadOptions.priorityDirection()).isEqualTo(new Vector3(-0.0D, 0.0D, 1.0D));
    }

    @Test
    void loadChunksAheadBuildsForwardBiasedChunkSet() {
        var world = new RecordingWorld();
        var location = world.at(8.0D, 64.0D, 8.0D, 0.0F, 0.0F);

        world.loadChunksAhead(location, 2, 1, 0);

        assertThat(world.lastLoadChunks).containsExactly(
                ChunkPos.of(1, 0),
                ChunkPos.of(0, 0),
                ChunkPos.of(-1, 0),
                ChunkPos.of(1, 1),
                ChunkPos.of(0, 1),
                ChunkPos.of(-1, 1),
                ChunkPos.of(1, 2),
                ChunkPos.of(0, 2),
                ChunkPos.of(-1, 2));
        assertThat(world.lastLoadOptions.order()).isEqualTo(ChunkOrder.FORWARD_FIRST);
    }

    @Test
    void operationConveniencesJoinAndCompletionCallback() {
        var result = ChunkBatchResult.empty();
        var seen = new ArrayList<ChunkBatchResult>();
        var progress = new ArrayList<ChunkBatchProgress>();
        var operation = RecordingWorld.completed(result);

        assertThat(operation.join()).isSameAs(result);
        assertThat(operation.onComplete(seen::add)).isSameAs(operation);
        assertThat(operation.onProgress(progress::add)).isSameAs(operation);
        assertThat(seen).containsExactly(result);
        assertThat(progress).containsExactly(new ChunkBatchProgress(0, 0, 0, 0, false, true));
    }

    private static class RecordingWorld extends TestWorld {
        private final List<ChunkRegion> loadRegions = new ArrayList<>();
        private List<ChunkPos> lastLoadChunks = List.of();
        private ChunkBatchOptions lastLoadOptions;
        private ChunkRegion lastForceRegion;
        private boolean lastForceLoaded;

        @Override
        public ChunkBatchOperation loadChunks(ChunkRegion region, ChunkBatchOptions options) {
            loadRegions.add(region);
            lastLoadOptions = options;
            return completed();
        }

        @Override
        public ChunkBatchOperation loadChunks(Iterable<ChunkPos> chunks, ChunkBatchOptions options) {
            lastLoadChunks = new ArrayList<>();
            chunks.forEach(lastLoadChunks::add);
            lastLoadOptions = options;
            return completed();
        }

        @Override
        public ChunkBatchOperation setChunksForceLoaded(ChunkRegion region, boolean forceLoaded, ChunkBatchOptions options) {
            lastForceRegion = region;
            lastForceLoaded = forceLoaded;
            return completed();
        }

        private static ChunkBatchOperation completed() {
            return completed(ChunkBatchResult.empty());
        }

        private static ChunkBatchOperation completed(ChunkBatchResult result) {
            return new ChunkBatchOperation() {
                @Override
                public CompletableFuture<ChunkBatchResult> future() {
                    return CompletableFuture.completedFuture(result);
                }

                @Override
                public ChunkBatchProgress progress() {
                    return new ChunkBatchProgress(0, 0, 0, 0, false, true);
                }

                @Override
                public boolean cancel() {
                    return false;
                }
            };
        }
    }

    private static class TestWorld implements World {

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
        public Block blockAt(int x, int y, int z) {
            throw new UnsupportedOperationException();
        }
    }
}
