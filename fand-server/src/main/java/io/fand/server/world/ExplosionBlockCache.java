package io.fand.server.world;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Per-explosion cache of block data observed by explosion rays.
 *
 * <p>Fand: direct-mapped explosion block caching is adapted from Lithium's
 * explosion block-raycast optimization (CaffeineMC), licensed under GNU
 * LGPLv3.
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

    private static final int CACHE_BITS = 19;
    private static final int CACHE_SIZE = 1 << CACHE_BITS;
    private static final int CACHE_MASK = CACHE_SIZE - 1;

    private final long[] keys = new long[CACHE_SIZE];
    private final int[] generations = new int[CACHE_SIZE];
    private final Entry[] entries = new Entry[CACHE_SIZE];
    private int generation = 1;
    private int size;

    public @Nullable Entry get(long packedPos) {
        int index = index(packedPos);
        return generations[index] == generation && keys[index] == packedPos ? entries[index] : null;
    }

    public Entry put(long packedPos, BlockState state, float resistance, boolean inWorldBounds) {
        int index = index(packedPos);
        Entry entry = entries[index];
        if (entry == null) {
            entry = new Entry();
            entries[index] = entry;
        }
        if (generations[index] != generation) {
            generations[index] = generation;
            size++;
        }
        keys[index] = packedPos;
        entry.set(state, resistance, inWorldBounds);
        return entry;
    }

    public void clear() {
        if (size != 0) {
            generation++;
            size = 0;
            if (generation == 0) {
                java.util.Arrays.fill(generations, 0);
                generation = 1;
            }
        }
    }

    int size() {
        return size;
    }

    private static int index(long packedPos) {
        return (int)HashCommon.mix(packedPos) & CACHE_MASK;
    }

    public static final class Entry {
        private @Nullable BlockState state;
        private float resistance;
        private boolean inWorldBounds;

        private void set(final @Nullable BlockState state, final float resistance, final boolean inWorldBounds) {
            this.state = state;
            this.resistance = resistance;
            this.inWorldBounds = inWorldBounds;
        }

        public @Nullable BlockState state() {
            return this.state;
        }

        public float resistance() {
            return this.resistance;
        }

        public boolean inWorldBounds() {
            return this.inWorldBounds;
        }

        public boolean hasResistance() {
            return !Float.isNaN(this.resistance);
        }
    }
}
