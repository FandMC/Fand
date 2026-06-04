package io.fand.server.config;

import java.nio.file.Path;

public final class FandConfig {

    @ConfigComment("Public-facing identity settings.")
    public final Identity identity = new Identity();

    @ConfigComment("Plugin runtime settings.")
    public final Plugins plugins = new Plugins();

    @ConfigComment("Async scheduler settings.")
    public final Scheduler scheduler = new Scheduler();

    public static FandConfig load(Path path) {
        return new YamlConfigLoader<>(FandConfig.class).load(path);
    }

    public static final class Identity {

        @ConfigComment("Brand reported by the server and plugin API.")
        public String brand = "Fand";
    }

    public static final class Plugins {

        @ConfigComment("Directory scanned for plugin jars and plugin data folders.")
        public String directory = "plugins";
    }

    public static final class Scheduler {

        @ConfigComment({
                "Number of async scheduler threads.",
                "Set to 0 to derive the value from available processors."
        })
        @ConfigRange(min = 0, max = 1024)
        public int asyncThreads = 0;
    }
}
