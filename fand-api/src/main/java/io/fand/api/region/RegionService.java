package io.fand.api.region;

import io.fand.api.world.Location;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Region lookup and flag registry service.
 */
public interface RegionService {

    Path rootDirectory();

    Collection<Region> regions();

    Optional<Region> region(Key key);

    boolean remove(Key key);

    Collection<Region> applicableRegions(Location location);

    Optional<Region> applicableRegion(Location location);

    Collection<RegionFlag<?>> flags();

    Optional<RegionFlag<?>> flag(Key key);

    boolean unregisterFlag(Key key);

    RegionRegistration register(RegionDefinition region);

    <T> RegionFlagRegistration registerFlag(RegionFlag<T> flag);

    default <T> RegionFlagRegistration registerFlag(Key key, RegionFlagCodec<T> codec, T defaultValue) {
        return registerFlag(RegionFlag.of(key, codec, defaultValue));
    }

    static RegionService empty() {
        return Empty.INSTANCE;
    }

    enum Empty implements RegionService {
        INSTANCE;

        @Override
        public Collection<Region> regions() {
            return java.util.List.of();
        }

        @Override
        public Path rootDirectory() {
            return Path.of("regions");
        }

        @Override
        public Optional<Region> region(Key key) {
            return Optional.empty();
        }

        @Override
        public boolean remove(Key key) {
            return false;
        }

        @Override
        public Collection<Region> applicableRegions(Location location) {
            return java.util.List.of();
        }

        @Override
        public Optional<Region> applicableRegion(Location location) {
            return Optional.empty();
        }

        @Override
        public Collection<RegionFlag<?>> flags() {
            return java.util.List.of();
        }

        @Override
        public Optional<RegionFlag<?>> flag(Key key) {
            return Optional.empty();
        }

        @Override
        public boolean unregisterFlag(Key key) {
            return false;
        }

        @Override
        public RegionRegistration register(RegionDefinition region) {
            throw new UnsupportedOperationException("Regions are not supported");
        }

        @Override
        public <T> RegionFlagRegistration registerFlag(RegionFlag<T> flag) {
            throw new UnsupportedOperationException("Region flags are not supported");
        }
    }
}
