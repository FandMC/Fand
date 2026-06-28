package io.fand.api.region;

import io.fand.api.world.Location;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolved flag value plus the ordered regions inspected to produce it.
 */
public record RegionFlagResolution<T>(
        RegionFlag<T> flag,
        Location location,
        Optional<T> value,
        boolean defaultValue,
        List<RegionFlagTrace<T>> trace
) {

    public RegionFlagResolution {
        flag = Objects.requireNonNull(flag, "flag");
        location = Objects.requireNonNull(location, "location");
        value = Objects.requireNonNull(value, "value");
        trace = List.copyOf(Objects.requireNonNull(trace, "trace"));
    }

    public T orElse(T fallback) {
        return value.orElse(fallback);
    }

    public boolean resolvedFromRegion() {
        return !defaultValue && value.isPresent();
    }
}
