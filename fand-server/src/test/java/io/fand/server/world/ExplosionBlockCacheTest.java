package io.fand.server.world;

import static org.assertj.core.api.Assertions.assertThat;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

final class ExplosionBlockCacheTest {

    @Test
    void missReturnsNullThenHitReturnsEntry() {
        var cache = new ExplosionBlockCache();
        long pos = BlockPos.asLong(1, 64, -3);

        assertThat(cache.get(pos)).isNull();

        net.minecraft.world.level.block.state.BlockState state = null;
        cache.put(pos, state, 6.0F, true);

        var entry = cache.get(pos);
        assertThat(entry).isNotNull();
        assertThat(entry.state()).isNull();
        assertThat(entry.resistance()).isEqualTo(6.0F);
        assertThat(entry.hasResistance()).isTrue();
        assertThat(entry.inWorldBounds()).isTrue();
    }

    @Test
    void nanResistanceMarksEmptyCalculatorResult() {
        var cache = new ExplosionBlockCache();
        long pos = BlockPos.asLong(0, 64, 0);

        cache.put(pos, null, Float.NaN, true);

        var entry = cache.get(pos);
        assertThat(entry).isNotNull();
        assertThat(entry.hasResistance()).isFalse();
    }

    @Test
    void outOfBoundsEntryIsCached() {
        var cache = new ExplosionBlockCache();
        long pos = BlockPos.asLong(0, 5000, 0);

        cache.put(pos, null, Float.NaN, false);

        var entry = cache.get(pos);
        assertThat(entry).isNotNull();
        assertThat(entry.inWorldBounds()).isFalse();
    }

    @Test
    void clearDropsAllEntries() {
        var cache = new ExplosionBlockCache();
        cache.put(BlockPos.asLong(0, 64, 0), null, 6.0F, true);
        cache.put(BlockPos.asLong(1, 64, 0), null, 6.0F, true);
        assertThat(cache.size()).isEqualTo(2);

        cache.clear();

        assertThat(cache.size()).isZero();
        assertThat(cache.get(BlockPos.asLong(0, 64, 0))).isNull();
    }
}
