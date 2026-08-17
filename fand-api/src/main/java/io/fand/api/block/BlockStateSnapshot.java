package io.fand.api.block;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable snapshot of a block type and its vanilla state properties. */
public record BlockStateSnapshot(BlockType type, Map<String, String> properties) {

    public BlockStateSnapshot {
        Objects.requireNonNull(type, "type");
        properties = Map.copyOf(Objects.requireNonNull(properties, "properties"));
    }

    public Optional<String> property(String name) {
        return Optional.ofNullable(properties.get(Objects.requireNonNull(name, "name")));
    }
}
