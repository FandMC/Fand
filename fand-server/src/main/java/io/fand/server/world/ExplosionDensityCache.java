package io.fand.server.world;

import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Per-level cache for explosion line-of-sight exposure
 * ({@link ServerExplosion#getSeenPercent}).
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

    private static final float MISS = -1.0F;

    private final Object2FloatOpenHashMap<Key> exposures = new Object2FloatOpenHashMap<>();

    public ExplosionDensityCache() {
        exposures.defaultReturnValue(MISS);
    }

    public float seenPercent(Vec3 center, Entity entity) {
        return seenPercent(center, entity.getBoundingBox(), () -> ServerExplosion.getSeenPercent(center, entity));
    }

    float seenPercent(Vec3 center, AABB box, ExposureFunction compute) {
        var key = new Key(
                Mth.floor(center.x),
                Mth.floor(center.y),
                Mth.floor(center.z),
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ
        );
        float cached = exposures.getFloat(key);
        if (cached != MISS) {
            return cached;
        }
        float computed = compute.compute();
        exposures.put(key, computed);
        return computed;
    }

    public void clear() {
        if (!exposures.isEmpty()) {
            exposures.clear();
        }
    }

    int size() {
        return exposures.size();
    }

    @FunctionalInterface
    interface ExposureFunction {
        float compute();
    }

    private record Key(
            int centerBlockX,
            int centerBlockY,
            int centerBlockZ,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
    }
}
