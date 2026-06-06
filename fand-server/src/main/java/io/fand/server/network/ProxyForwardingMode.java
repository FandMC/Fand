package io.fand.server.network;

import io.fand.server.config.ConfigException;
import java.util.Locale;

public enum ProxyForwardingMode {
    NONE("none", false),
    BUNGEE_LEGACY("bungee-legacy", false),
    VELOCITY_MODERN("velocity-modern", true);

    private final String configValue;
    private final boolean requiresSecret;

    ProxyForwardingMode(String configValue, boolean requiresSecret) {
        this.configValue = configValue;
        this.requiresSecret = requiresSecret;
    }

    public String configValue() {
        return configValue;
    }

    public boolean requiresSecret() {
        return requiresSecret;
    }

    public static ProxyForwardingMode fromConfig(String value) {
        var normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "none", "off", "false" -> NONE;
            case "bc", "bungee", "bungeecord", "bungee-legacy" -> BUNGEE_LEGACY;
            case "velocity", "velocity-modern" -> VELOCITY_MODERN;
            default -> throw new ConfigException(
                    "network.forwarding.mode must be one of: none, bungee-legacy, velocity-modern (aliases: bc, bungee, bungeecord, velocity)");
        };
    }
}
