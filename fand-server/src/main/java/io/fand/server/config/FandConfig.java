package io.fand.server.config;

import io.fand.server.network.ProxyForwardingMode;
import java.nio.file.Path;

public final class FandConfig {

    @ConfigComment("Public-facing identity settings.")
    public final Identity identity = new Identity();

    @ConfigComment("Plugin runtime settings.")
    public final Plugins plugins = new Plugins();

    @ConfigComment("Async scheduler settings.")
    public final Scheduler scheduler = new Scheduler();

    @ConfigComment("Network and proxy settings.")
    public final Network network = new Network();

    public static FandConfig load(Path path) {
        var config = new YamlConfigLoader<>(FandConfig.class).load(path);
        validate(config);
        return config;
    }

    private static void validate(FandConfig config) {
        var mode = ProxyForwardingMode.fromConfig(config.network.forwarding.mode);
        if (mode.requiresSecret() && config.network.forwarding.secret.isBlank()) {
            throw new ConfigException("network.forwarding.secret must be set when network.forwarding.mode is " + mode.configValue());
        }
    }

    public static final class Identity {

        @ConfigComment("Brand reported by the server and plugin API.")
        public String brand = "Fand";
    }

    public static final class Plugins {

        @ConfigComment("Directory scanned for plugin jars and plugin data folders.")
        public String directory = "plugins";

        @ConfigComment("Continue boot if a plugin fails during discovery, construction, or onLoad.")
        public boolean continueOnLoadFailure = false;

        @ConfigComment("Continue boot if a plugin fails during onEnable.")
        public boolean continueOnEnableFailure = false;

        @ConfigComment("Log a summary after plugin load and enable phases.")
        public boolean logSummary = true;
    }

    public static final class Scheduler {

        @ConfigComment({
                "Number of async scheduler threads.",
                "Set to 0 to derive the value from available processors."
        })
        @ConfigRange(min = 0, max = 1024)
        public int asyncThreads = 0;
    }

    public static final class Network {

        @ConfigComment("Proxy player information forwarding settings.")
        public final Forwarding forwarding = new Forwarding();
    }

    public static final class Forwarding {

        @ConfigComment({
                "Proxy forwarding mode.",
                "Supported values: none, bungee-legacy, velocity-modern."
        })
        public String mode = "none";

        @ConfigComment("Shared secret used by velocity-modern forwarding.")
        public String secret = "";
    }
}
