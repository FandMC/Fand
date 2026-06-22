package io.fand.api.structure;

import java.util.Objects;
import net.kyori.adventure.key.Key;

public record StructureSetEntry(Key structure, int weight) {

    public StructureSetEntry {
        Objects.requireNonNull(structure, "structure");
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
    }
}
