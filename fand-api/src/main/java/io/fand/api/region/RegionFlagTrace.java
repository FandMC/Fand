package io.fand.api.region;

import java.util.Objects;
import java.util.Optional;

/**
 * One region inspected while resolving a flag.
 */
public record RegionFlagTrace<T>(
        Region region,
        Optional<T> value,
        boolean inherited
) {

    public RegionFlagTrace {
        region = Objects.requireNonNull(region, "region");
        value = Objects.requireNonNull(value, "value");
    }

    public boolean defined() {
        return value.isPresent();
    }
}
