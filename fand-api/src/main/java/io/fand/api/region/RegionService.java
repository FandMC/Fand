package io.fand.api.region;

import io.fand.api.world.Location;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

    /**
     * Returns regions containing {@code location} in flag resolution order:
     * higher protection priority first, then smaller volume first, then most
     * recent registration first.
     */
    Collection<Region> applicableRegions(Location location);

    /**
     * Returns the first region from {@link #applicableRegions(Location)}.
     */
    Optional<Region> applicableRegion(Location location);

    /**
     * Resolves {@code flag} by walking {@link #applicableRegions(Location)} in
     * order. Each region checks its explicit flag first, then its parent chain.
     * The first explicit value wins, including a value inherited from a parent;
     * lower-priority overlapping regions are not consulted after a parent match.
     */
    default <T> RegionFlagResolution<T> resolveFlag(Location location, RegionFlag<T> flag) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(flag, "flag");
        var trace = new ArrayList<RegionFlagTrace<T>>();
        for (var region : applicableRegions(location)) {
            var resolved = resolveFlagFromRegion(region, flag, false, new HashSet<>(), trace);
            if (resolved.isPresent()) {
                return new RegionFlagResolution<>(flag, location, resolved, false, trace);
            }
        }
        return new RegionFlagResolution<>(flag, location, Optional.ofNullable(flag.defaultValue()), true, trace);
    }

    Collection<RegionFlag<?>> flags();

    Optional<RegionFlag<?>> flag(Key key);

    boolean unregisterFlag(Key key);

    RegionRegistration register(RegionDefinition region);

    <T> RegionFlagRegistration registerFlag(RegionFlag<T> flag);

    default <T> RegionFlagRegistration registerFlag(Key key, RegionFlagCodec<T> codec, T defaultValue) {
        return registerFlag(RegionFlag.of(key, codec, defaultValue));
    }

    private <T> Optional<T> resolveFlagFromRegion(
            Region region,
            RegionFlag<T> flag,
            boolean inherited,
            HashSet<Key> visited,
            List<RegionFlagTrace<T>> trace
    ) {
        if (!visited.add(region.key())) {
            return Optional.empty();
        }
        var value = region.explicitFlag(flag);
        trace.add(new RegionFlagTrace<>(region, value, inherited));
        if (value.isPresent()) {
            return value;
        }
        return region.protection().parent()
                .flatMap(this::region)
                .flatMap(parent -> resolveFlagFromRegion(parent, flag, true, visited, trace));
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
