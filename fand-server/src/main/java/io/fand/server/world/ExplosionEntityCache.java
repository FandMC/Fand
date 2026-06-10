package io.fand.server.world;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Per-level cache for explosion entity queries.
 * <p>
 * Caches the result of {@code level.getEntities(source, aabb)} keyed by the
 * compound of the floor of the explosion center, the double radius, and the
 * source entity's UUID. This avoids repeated expensive section traversals when
 * many explosions occur in the same tick with similar centers, while ensuring
 * explosions with different radii or different source entities never share a
 * stale cached result.
 * <p>
 * The {@link io.fand.server.hooks.FandHooks#explosionEntityCacheEnabled()
 * explosionEntityCache} config flag gates caching; when disabled every call
 * computes fresh to match vanilla behaviour.
 */
public final class ExplosionEntityCache {

    private final Object2ObjectOpenHashMap<Key, List<Entity>> cache = new Object2ObjectOpenHashMap<>();

    public List<Entity> getEntities(net.minecraft.server.level.ServerLevel level,
                                    Vec3 center,
                                    double doubleRadius,
                                    @Nullable Entity source) {
        if (!io.fand.server.hooks.FandHooks.explosionEntityCacheEnabled()) {
            return computeEntities(level, center, doubleRadius, source);
        }
        var key = new Key(
                Mth.floor(center.x),
                Mth.floor(center.y),
                Mth.floor(center.z),
                doubleRadius,
                source == null ? null : source.getUUID());
        return cache.computeIfAbsent(key, k -> computeEntities(level, center, k.doubleRadius, source));
    }

    private static List<Entity> computeEntities(net.minecraft.server.level.ServerLevel level,
                                                 Vec3 center,
                                                 double doubleRadius,
                                                 @Nullable Entity source) {
        int x0 = Mth.floor(center.x - doubleRadius - 1.0);
        int x1 = Mth.floor(center.x + doubleRadius + 1.0);
        int y0 = Mth.floor(center.y - doubleRadius - 1.0);
        int y1 = Mth.floor(center.y + doubleRadius + 1.0);
        int z0 = Mth.floor(center.z - doubleRadius - 1.0);
        int z1 = Mth.floor(center.z + doubleRadius + 1.0);
        AABB aabb = new AABB(x0, y0, z0, x1, y1, z1);
        return level.getEntities(source, aabb);
    }

    public void clear() {
        cache.clear();
    }

    private record Key(
            int centerBlockX,
            int centerBlockY,
            int centerBlockZ,
            double doubleRadius,
            @Nullable UUID sourceUuid
    ) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key that)) return false;
            return centerBlockX == that.centerBlockX
                    && centerBlockY == that.centerBlockY
                    && centerBlockZ == that.centerBlockZ
                    && Double.compare(doubleRadius, that.doubleRadius) == 0
                    && Objects.equals(sourceUuid, that.sourceUuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(centerBlockX, centerBlockY, centerBlockZ, doubleRadius, sourceUuid);
        }
    }
}