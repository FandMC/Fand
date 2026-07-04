package io.fand.server.redstone;

import io.fand.server.config.ConfigException;
import java.util.Locale;

public enum RedstoneJitMode {
    OFF(false, false),
    PROFILE(true, false),
    SHADOW(true, true),
    INTERPRETER(true, true),
    HOT(true, true);

    private final boolean profilingEnabled;
    private final boolean shadowEnabled;

    RedstoneJitMode(boolean profilingEnabled, boolean shadowEnabled) {
        this.profilingEnabled = profilingEnabled;
        this.shadowEnabled = shadowEnabled;
    }

    public boolean profilingEnabled() {
        return profilingEnabled;
    }

    public boolean shadowEnabled() {
        return shadowEnabled;
    }

    public boolean probeEnabled() {
        return this == PROFILE || this == SHADOW || this == INTERPRETER;
    }

    public boolean executorEnabled() {
        return this == INTERPRETER || this == HOT;
    }

    public static RedstoneJitMode fromConfig(String value) {
        if (value == null) {
            throw new ConfigException("performance.redstoneJitMode must be one of: off, profile, shadow, interpreter, hot");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "off", "false" -> OFF;
            case "profile" -> PROFILE;
            case "shadow" -> SHADOW;
            case "interpreter" -> INTERPRETER;
            case "hot" -> HOT;
            default -> throw new ConfigException("performance.redstoneJitMode must be one of: off, profile, shadow, interpreter, hot");
        };
    }
}
