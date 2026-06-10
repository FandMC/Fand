package io.fand.server.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Per-level cache for explosion entity queries.
 * <p>
 * Caches the result of {@code level.getEntities(source, aabb)} keyed by the
 * floor of the explosion center. This avoids repeated expensive section
 * traversals when many explosions occur in the same tick with similar centers.
 */
public final class ExplosionEntityCache {

    private final Long2ObjectMap<List<Entity>> cache = new Long2ObjectOpenHashMap<>();

    public List<Entity> getEntities(net.minecraft.server.level.ServerLevel level,
                                    Vec3 center,
                                    double doubleRadius,
                                    @Nullable Entity source) {
        long key = BlockPos.asLong(Mth.floor(center.x), Mth.floor(center.y), Mth.floor(center.z));
        return cache.computeIfAbsent(key, k -> {
            int x0 = Mth.floor(center.x - doubleRadius - 1.0);
            int x1 = Mth.floor(center.x + doubleRadius + 1.0);
            int y0 = Mth.floor(center.y - doubleRadius - 1.0);
            int y1 = Mth.floor(center.y + doubleRadius + 1.0);
            int z0 = Mth.floor(center.z - doubleRadius - 1.0);
            int z1 = Mth.floor(center.z + doubleRadius + 1.0);
            AABB aabb = new AABB(x0, y0, z0, x1, y1, z1);
            return level.getEntities(source, aabb);
        });
    }

    public void clear() {
        cache.clear();
    }
}