package io.fand.api.config;

import java.nio.file.Path;

/**
 * A persistent configuration document. Reads and writes are not thread-safe;
 * synchronise externally if accessed from multiple threads.
 */
public interface Configuration extends ConfigurationSection {

    /** Filesystem location backing this document. */
    Path file();

    /**
     * Re-reads {@link #file()} into memory, discarding any unsaved changes.
     * @throws ConfigurationException if the file cannot be read or parsed
     */
    void reload();

    /**
     * Writes the current in-memory state to {@link #file()} atomically.
     * @throws ConfigurationException if the file cannot be written
     */
    void save();
}
