package io.fand.server.config;

import java.util.List;

public record ConfigReloadResult(
        List<String> hotApplied,
        List<String> requiresRestart
) {
    public ConfigReloadResult {
        hotApplied = List.copyOf(hotApplied);
        requiresRestart = List.copyOf(requiresRestart);
    }

    public boolean changed() {
        return !hotApplied.isEmpty() || !requiresRestart.isEmpty();
    }

    public boolean restartRequired() {
        return !requiresRestart.isEmpty();
    }
}
