package io.fand.api.world;

import java.nio.file.Path;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Reusable saved-world snapshot.
 *
 * <p>Memory snapshots are backed by server-managed temporary files and should be
 * closed when no longer needed. File snapshots are owned by the caller.
 */
public interface WorldSnapshot extends AutoCloseable {

    Key sourceWorld();

    Path path();

    boolean memory();

    @Nullable Path persistentPath();

    @Override
    void close();
}
