package io.fand.server.world;

import io.fand.api.world.WorldSnapshot;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

public final class FandWorldSnapshot implements WorldSnapshot {

    private final Key sourceWorld;
    private final Path path;
    private final boolean memory;
    private final @Nullable Path persistentPath;
    private volatile boolean closed;

    public FandWorldSnapshot(Key sourceWorld, Path path, boolean memory, @Nullable Path persistentPath) {
        this.sourceWorld = java.util.Objects.requireNonNull(sourceWorld, "sourceWorld");
        this.path = java.util.Objects.requireNonNull(path, "path");
        this.memory = memory;
        this.persistentPath = persistentPath;
    }

    @Override
    public Key sourceWorld() {
        return sourceWorld;
    }

    @Override
    public Path path() {
        return path;
    }

    @Override
    public boolean memory() {
        return memory;
    }

    @Override
    public @Nullable Path persistentPath() {
        return persistentPath;
    }

    @Override
    public void close() {
        if (!memory || closed) {
            return;
        }
        closed = true;
        try {
            WorldFileOperations.deleteRecursively(path);
        } catch (java.io.IOException failure) {
            throw new UncheckedIOException("Failed to delete memory world snapshot " + path, failure);
        }
    }
}
