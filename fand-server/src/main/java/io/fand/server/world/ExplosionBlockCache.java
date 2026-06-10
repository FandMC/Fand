package io.fand.server.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Per-explosion cache of block data observed by explosion rays.
 *
 * <p>A single explosion traces 1352 rays that step through largely the same
 * sphere of blocks (~5x revisit factor), and no block mutates between ray
 * steps — vanilla only destroys blocks after all rays finish. Caching
 * (state, explosion resistance, world-bounds) per position is therefore
 * exactly vanilla-equivalent within one explosion. The patched
 * {@link net.minecraft.world.level.ServerExplosion} clears this cache at the
 * start of every explosion, so entries never leak across explosions.
 *
 * <p>Resistance is stored as a primitive float with {@link Float#NaN}
 * standing in for "calculator returned empty" (air without fluid).
 *
 * <p>Not thread-safe: confined to the owning level's tick thread.
 */
public final class ExplosionBlockCache {

    private final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap<>();

    public @Nullable Entry get(long packedPos) {
        return entries.get(packedPos);
    }

    public Entry put(long packedPos, BlockState state, float resistance, boolean inWorldBounds) {
        var entry = new Entry(state, resistance, inWorldBounds);
        entries.put(packedPos, entry);
        return entry;
    }

    public void clear() {
        if (!entries.isEmpty()) {
            entries.clear();
        }
    }

    int size() {
        return entries.size();
    }

    public record Entry(BlockState state, float resistance, boolean inWorldBounds) {

        public boolean hasResistance() {
            return !Float.isNaN(resistance);
        }
    }
}
