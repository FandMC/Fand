package io.fand.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BuildInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildInfo.class);

    public static final String VERSION;
    public static final String MINECRAFT_VERSION;

    static {
        var properties = propertiesFromResource();
        VERSION = properties.getProperty("version", "0.0.0-dev");
        MINECRAFT_VERSION = properties.getProperty("minecraftVersion", "unknown");
    }

    private BuildInfo() {
    }

    private static Properties propertiesFromResource() {
        try (var input = BuildInfo.class.getResourceAsStream("/fand-build.properties")) {
            return loadProperties(input);
        } catch (IOException failure) {
            LOGGER.warn("Failed to close Fand build metadata", failure);
            return new Properties();
        }
    }

    static Properties loadProperties(@Nullable InputStream input) {
        var properties = new Properties();
        if (input == null) {
            return properties;
        }
        try {
            properties.load(input);
        } catch (IOException failure) {
            LOGGER.warn("Failed to read Fand build metadata", failure);
        }
        return properties;
    }
}
