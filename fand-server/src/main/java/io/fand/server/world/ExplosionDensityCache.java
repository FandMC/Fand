package io.fand.server.world;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Per-level cache for explosion line-of-sight exposure
 * ({@link ServerExplosion#getSeenPercent}).
 *
 * <p>Fand: this direct-mapped exposure cache is part of the explosion
 * optimization work adapted from Lithium's explosion raycast optimizations
 * (CaffeineMC), licensed under GNU LGPLv3.
 *
 * <p>Vanilla re-traces ~27 block-stepping rays per nearby entity per
 * explosion. When many explosions detonate in the same tick (TNT chains and
 * cannons), most of those traces are identical. The cache key quantises the
 * explosion center to its block position and pairs it with the entity's exact
 * bounding box, so same-block explosions reuse each other's results — the same
 * trade-off Paper's optimize-explosions makes: exposure may be off by up to a
 * block of center offset, and may predate block changes from an explosion
 * earlier in the same tick. The patched {@link
 * net.minecraft.server.level.ServerLevel} clears the cache at the start of
 * every level tick.
 *
 * <p>Not thread-safe: confined to the owning level's tick thread.
 */
public final class ExplosionDensityCache {

    private static final int CACHE_BITS = 15;
    private static final int CACHE_SIZE = 1 << CACHE_BITS;
    private static final int CACHE_MASK = CACHE_SIZE - 1;
    private final long[] keyHashes = new long[CACHE_SIZE];
    private final int[] centerBlockX = new int[CACHE_SIZE];
    private final int[] centerBlockY = new int[CACHE_SIZE];
    private final int[] centerBlockZ = new int[CACHE_SIZE];
    private final long[] minX = new long[CACHE_SIZE];
    private final long[] minY = new long[CACHE_SIZE];
    private final long[] minZ = new long[CACHE_SIZE];
    private final long[] maxX = new long[CACHE_SIZE];
    private final long[] maxY = new long[CACHE_SIZE];
    private final long[] maxZ = new long[CACHE_SIZE];
    private final float[] values = new float[CACHE_SIZE];
    private final int[] generations = new int[CACHE_SIZE];
    private int generation = 1;
    private int size;

    public float seenPercent(Vec3 center, Entity entity) {
        return seenPercent(center, entity.getBoundingBox(), () -> ServerExplosion.getSeenPercent(center, entity));
    }

    public float seenPercent(Vec3 center, AABB box, ExposureFunction compute) {
        int x = Mth.floor(center.x);
        int y = Mth.floor(center.y);
        int z = Mth.floor(center.z);
        long minXBits = Double.doubleToLongBits(box.minX);
        long minYBits = Double.doubleToLongBits(box.minY);
        long minZBits = Double.doubleToLongBits(box.minZ);
        long maxXBits = Double.doubleToLongBits(box.maxX);
        long maxYBits = Double.doubleToLongBits(box.maxY);
        long maxZBits = Double.doubleToLongBits(box.maxZ);
        long hash = hash(x, y, z, minXBits, minYBits, minZBits, maxXBits, maxYBits, maxZBits);
        int index = (int)hash & CACHE_MASK;
        if (generations[index] == generation
                && keyHashes[index] == hash
                && centerBlockX[index] == x
                && centerBlockY[index] == y
                && centerBlockZ[index] == z
                && minX[index] == minXBits
                && minY[index] == minYBits
                && minZ[index] == minZBits
                && maxX[index] == maxXBits
                && maxY[index] == maxYBits
                && maxZ[index] == maxZBits) {
            return values[index];
        }
        float computed = compute.compute();
        if (generations[index] != generation) {
            generations[index] = generation;
            size++;
        }
        keyHashes[index] = hash;
        centerBlockX[index] = x;
        centerBlockY[index] = y;
        centerBlockZ[index] = z;
        minX[index] = minXBits;
        minY[index] = minYBits;
        minZ[index] = minZBits;
        maxX[index] = maxXBits;
        maxY[index] = maxYBits;
        maxZ[index] = maxZBits;
        values[index] = computed;
        return computed;
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

    @FunctionalInterface
    public interface ExposureFunction {
        float compute();
    }

    private static long hash(
            int centerBlockX,
            int centerBlockY,
            int centerBlockZ,
            long minX,
            long minY,
            long minZ,
            long maxX,
            long maxY,
            long maxZ
    ) {
        long hash = centerBlockX;
        hash = 31L * hash + centerBlockY;
        hash = 31L * hash + centerBlockZ;
        hash = 31L * hash + minX;
        hash = 31L * hash + minY;
        hash = 31L * hash + minZ;
        hash = 31L * hash + maxX;
        hash = 31L * hash + maxY;
        hash = 31L * hash + maxZ;
        return HashCommon.mix(hash);
    }
}
