package io.fand.api.config;

import java.io.InputStream;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * Loads and creates persistent configuration documents in supported formats.
 */
public interface ConfigurationService {

    /**
     * Loads a configuration document, detecting the format from the extension.
     *
     * @throws ConfigurationException if the file cannot be read, parsed, or detected
     */
    Configuration load(Path file);

    /**
     * Loads a configuration document with an explicit format.
     *
     * @throws ConfigurationException if the file cannot be read or parsed
     */
    Configuration load(Path file, ConfigurationFormat format);

    /**
     * Materialises {@code defaults} into {@code file} when missing, then loads it.
     * The format is detected from the extension.
     */
    Configuration loadOrCopyDefault(Path file, @Nullable InputStream defaults);

    /**
     * Materialises {@code defaults} into {@code file} when missing, then loads it
     * with an explicit format.
     */
    Configuration loadOrCopyDefault(Path file, ConfigurationFormat format, @Nullable InputStream defaults);
}
