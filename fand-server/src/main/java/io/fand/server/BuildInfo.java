package io.fand.server;

import java.io.IOException;
import java.util.Properties;

public final class BuildInfo {

    public static final String VERSION;
    public static final String MINECRAFT_VERSION;

    static {
        var properties = new Properties();
        try (var input = BuildInfo.class.getResourceAsStream("/fand-build.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
        }
        VERSION = properties.getProperty("version", "0.0.0-dev");
        MINECRAFT_VERSION = properties.getProperty("minecraftVersion", "unknown");
    }

    private BuildInfo() {
    }
}
