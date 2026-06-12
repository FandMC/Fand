package io.fand.server.config;

import io.fand.api.config.ConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class FileBackedConfiguration extends MapBackedConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBackedConfiguration.class);

    private final Path file;
    private final String formatName;

    protected FileBackedConfiguration(Path file, String formatName) {
        this.file = Objects.requireNonNull(file, "file");
        this.formatName = Objects.requireNonNull(formatName, "formatName");
    }

    @Override
    public final Path file() {
        return file;
    }

    @Override
    public final void reload() {
        if (!Files.exists(file)) {
            clearRoot();
            return;
        }
        try {
            replaceRoot(readFile(file));
        } catch (IOException ex) {
            throw new ConfigurationException("Failed to read config " + file, ex);
        } catch (RuntimeException ex) {
            if (ex instanceof ConfigurationException configurationException) {
                throw configurationException;
            }
            throw new ConfigurationException("Failed to parse " + formatName + " config " + file + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public final void save() {
        Path tmp;
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            tmp = Files.createTempFile(
                    file.getParent() == null ? Path.of(".") : file.getParent(),
                    file.getFileName().toString(),
                    ".tmp");
        } catch (IOException ex) {
            throw new ConfigurationException("Failed to allocate temp file for " + file, ex);
        }
        try {
            writeFile(tmp, new LinkedHashMap<>(rootValues()));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException cleanupFailure) {
                LOGGER.warn("Failed to delete temp config file {}", tmp, cleanupFailure);
            }
            throw new ConfigurationException("Failed to save config " + file, ex);
        }
    }

    protected abstract Map<String, Object> readFile(Path file) throws IOException;

    protected abstract void writeFile(Path file, Map<String, Object> values) throws IOException;
}
