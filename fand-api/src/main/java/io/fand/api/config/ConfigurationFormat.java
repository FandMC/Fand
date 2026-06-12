package io.fand.api.config;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * File formats supported by the generic configuration API.
 */
public enum ConfigurationFormat {
    YAML("yml", "yaml"),
    JSON("json"),
    TOML("toml"),
    PROPERTIES("properties", "props");

    private final Set<String> extensions;

    ConfigurationFormat(String... extensions) {
        this.extensions = Set.copyOf(Arrays.asList(extensions));
    }

    /**
     * File extensions mapped to this format, without the leading dot.
     */
    public Set<String> extensions() {
        return extensions;
    }

    /**
     * Detects the configuration format from {@code file}'s extension.
     *
     * @throws ConfigurationException if the extension is not supported
     */
    public static ConfigurationFormat detect(Path file) {
        return findByExtension(file)
                .orElseThrow(() -> new ConfigurationException("Unsupported configuration file extension: " + file));
    }

    /**
     * Finds a configuration format by {@code file}'s extension.
     */
    public static Optional<ConfigurationFormat> findByExtension(Path file) {
        var name = file.getFileName();
        if (name == null) {
            return Optional.empty();
        }
        return findByExtension(name.toString());
    }

    /**
     * Finds a configuration format by extension or file name.
     */
    public static Optional<ConfigurationFormat> findByExtension(String fileNameOrExtension) {
        var extension = extensionOf(fileNameOrExtension);
        if (extension.isEmpty()) {
            return Optional.empty();
        }
        for (var format : values()) {
            if (format.extensions.contains(extension)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    private static String extensionOf(String fileNameOrExtension) {
        var value = fileNameOrExtension.toLowerCase(Locale.ROOT);
        var lastSlash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        var lastDot = value.lastIndexOf('.');
        if (lastDot > lastSlash) {
            return value.substring(lastDot + 1);
        }
        return value.startsWith(".") ? value.substring(1) : value;
    }
}
