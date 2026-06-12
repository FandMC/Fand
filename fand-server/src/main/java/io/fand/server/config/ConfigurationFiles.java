package io.fand.server.config;

import io.fand.api.config.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

final class ConfigurationFiles {

    private ConfigurationFiles() {
    }

    static void materialiseDefault(Path file, @Nullable InputStream defaults) {
        if (Files.exists(file)) {
            return;
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            if (defaults != null) {
                Files.copy(defaults, file);
            } else {
                Files.createFile(file);
            }
        } catch (IOException ex) {
            throw new ConfigurationException("Failed to materialise default config at " + file, ex);
        }
    }
}
