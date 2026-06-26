package io.fand.server.plugin;

import io.fand.api.region.Region;
import io.fand.api.region.RegionDefinition;
import io.fand.api.region.RegionFlag;
import io.fand.api.region.RegionFlagRegistration;
import io.fand.api.region.RegionRegistration;
import io.fand.api.region.RegionService;
import io.fand.api.world.Location;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public final class PluginRegionService implements RegionService {

    private final RegionService delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginRegionService(RegionService delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public Path rootDirectory() {
        return delegate.rootDirectory();
    }

    @Override
    public Collection<Region> regions() {
        return delegate.regions().stream()
                .filter(region -> namespace.equals(region.key().namespace()))
                .toList();
    }

    @Override
    public Optional<Region> region(Key key) {
        return delegate.region(scopedKey(key))
                .filter(region -> namespace.equals(region.key().namespace()));
    }

    @Override
    public boolean remove(Key key) {
        return delegate.remove(scopedKey(key));
    }

    @Override
    public Collection<Region> applicableRegions(Location location) {
        return delegate.applicableRegions(location).stream()
                .filter(region -> namespace.equals(region.key().namespace()))
                .toList();
    }

    @Override
    public Optional<Region> applicableRegion(Location location) {
        return delegate.applicableRegion(location)
                .filter(region -> namespace.equals(region.key().namespace()));
    }

    @Override
    public Collection<RegionFlag<?>> flags() {
        return delegate.flags().stream()
                .filter(flag -> namespace.equals(flag.key().namespace()))
                .toList();
    }

    @Override
    public Optional<RegionFlag<?>> flag(Key key) {
        return delegate.flag(scopedKey(key))
                .filter(flag -> namespace.equals(flag.key().namespace()));
    }

    @Override
    public boolean unregisterFlag(Key key) {
        return delegate.unregisterFlag(scopedKey(key));
    }

    @Override
    public RegionRegistration register(RegionDefinition region) {
        Objects.requireNonNull(region, "region");
        return tracker.track(delegate.register(new RegionDefinition(
                scopedKey(region.key()),
                scopedWorld(region.world()),
                region.region(),
                region.flags())));
    }

    @Override
    public <T> RegionFlagRegistration registerFlag(RegionFlag<T> flag) {
        Objects.requireNonNull(flag, "flag");
        return tracker.track(delegate.registerFlag(new RegionFlag<>(
                scopedKey(flag.key()),
                flag.codec(),
                flag.defaultValue())));
    }

    private Key scopedKey(Key key) {
        Objects.requireNonNull(key, "key");
        if (namespace.equals(key.namespace())) {
            return key;
        }
        return Key.key(namespace, key.value());
    }

    private Key scopedWorld(Key key) {
        Objects.requireNonNull(key, "key");
        return key;
    }
}
