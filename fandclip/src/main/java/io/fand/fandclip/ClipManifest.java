package io.fand.fandclip;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Build-time metadata embedded in the fandclip jar. Populated by the {@code fandclip}
 * Gradle build via {@code clip-manifest.properties}.
 */
final class ClipManifest {

    private static final Properties PROPS = load();

    private ClipManifest() {}

    static String minecraftVersion() {
        return PROPS.getProperty("minecraftVersion", "unknown");
    }

    static String fandVersion() {
        return PROPS.getProperty("fandVersion", "0.0.0-dev");
    }

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = ClipManifest.class.getResourceAsStream("/clip-manifest.properties")) {
            if (in != null) p.load(in);
        } catch (IOException ignored) {
            // fall back to defaults
        }
        return p;
    }
}
