package io.fand.api.structure;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public record StructureProjection(
        StructureFormat format,
        byte[] data,
        Optional<Key> sourceKey,
        Optional<String> name
) {

    public StructureProjection {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(data, "data");
        sourceKey = Objects.requireNonNull(sourceKey, "sourceKey");
        name = Objects.requireNonNull(name, "name");
        data = Arrays.copyOf(data, data.length);
    }

    @Override
    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }

    public static StructureProjection of(StructureFormat format, byte[] data) {
        return new StructureProjection(format, data, Optional.empty(), Optional.empty());
    }

    public StructureProjection withSourceKey(Key sourceKey) {
        return new StructureProjection(format, data, Optional.of(sourceKey), name);
    }

    public StructureProjection withName(String name) {
        return new StructureProjection(format, data, sourceKey, Optional.of(name));
    }
}
