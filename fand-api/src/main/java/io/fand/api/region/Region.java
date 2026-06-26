package io.fand.api.region;

import io.fand.api.world.BlockRegion;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * A named region in a world.
 */
public record Region(
        Key key,
        Key world,
        BlockRegion region,
        Map<Key, com.google.gson.JsonElement> flags
) {

    public Region {
        Objects.requireNonNull(key, "key");
        world = Objects.requireNonNull(world, "world");
        region = Objects.requireNonNull(region, "region");
        flags = Map.copyOf(Objects.requireNonNull(flags, "flags"));
    }

    public static Region of(Key key, String world, BlockRegion region) {
        return new Region(key, Key.key(world), region, Map.of());
    }

    public static Region of(Key key, Key world, BlockRegion region) {
        return new Region(key, world, region, Map.of());
    }

    public boolean contains(io.fand.api.world.Location location) {
        Objects.requireNonNull(location, "location");
        return world.equals(location.world().key())
                && location.blockX() >= region.minX()
                && location.blockX() <= region.maxX()
                && location.blockY() >= region.minY()
                && location.blockY() <= region.maxY()
                && location.blockZ() >= region.minZ()
                && location.blockZ() <= region.maxZ();
    }

    public <T> Optional<T> flag(RegionFlag<T> flag) {
        Objects.requireNonNull(flag, "flag");
        var value = flags.get(flag.key());
        return value == null ? Optional.ofNullable(flag.defaultValue()) : Optional.of(flag.decode(value));
    }

    public boolean flag(RegionFlag<Boolean> flag, boolean fallback) {
        return flag(flag).orElse(fallback);
    }

}
