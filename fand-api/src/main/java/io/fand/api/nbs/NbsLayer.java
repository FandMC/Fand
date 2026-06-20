package io.fand.api.nbs;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Layer metadata from a Note Block Studio song. */
public record NbsLayer(
        int index,
        @Nullable String name,
        NbsLayerStatus status,
        int volume,
        int panning
) {

    public NbsLayer {
        status = java.util.Objects.requireNonNull(status, "status");
    }

    public Optional<String> nameOptional() {
        return Optional.ofNullable(name);
    }
}
