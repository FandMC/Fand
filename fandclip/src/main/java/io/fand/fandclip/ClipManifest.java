package io.fand.fandclip;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Build-time metadata embedded in the fandclip jar. Populated by the {@code fandclip}
 * Gradle build via {@code clip-manifest.properties}.
 */
final class ClipManifest {

    private static final System.Logger LOGGER = System.getLogger(ClipManifest.class.getName());
    private static final Properties PROPS = loadResource();

    private ClipManifest() {}

    static String minecraftVersion() {
        return PROPS.getProperty("minecraftVersion", "unknown");
    }

    static String fandVersion() {
        return PROPS.getProperty("fandVersion", "0.0.0-dev");
    }

    private static Properties loadResource() {
        try (InputStream in = ClipManifest.class.getResourceAsStream("/clip-manifest.properties")) {
            return load(in);
        } catch (IOException failure) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to close fandclip manifest metadata", failure);
            return new Properties();
        }
    }

    static Properties load(InputStream input) {
        var properties = new Properties();
        if (input == null) {
            return properties;
        }
        try {
            properties.load(input);
        } catch (IOException failure) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to read fandclip manifest metadata", failure);
        }
        return properties;
    }
}
