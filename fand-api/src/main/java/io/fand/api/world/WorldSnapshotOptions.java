package io.fand.api.world;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * Destination options for saved-world snapshots.
 */
public record WorldSnapshotOptions(boolean memory, @Nullable Path path) {

    private static final WorldSnapshotOptions IN_MEMORY = new WorldSnapshotOptions(true, null);

    public WorldSnapshotOptions {
        if (!memory && path == null) {
            throw new IllegalArgumentException("file snapshots require a path");
        }
    }

    public static WorldSnapshotOptions inMemory() {
        return IN_MEMORY;
    }

    public static WorldSnapshotOptions file(Path path) {
        return new WorldSnapshotOptions(false, java.util.Objects.requireNonNull(path, "path"));
    }
}
