package io.fand.server.config;

import java.util.ArrayList;
import java.util.List;

public final class ConfigReloadMessages {

    private ConfigReloadMessages() {
    }

    public static List<String> lines(ConfigReloadResult result) {
        var lines = new ArrayList<String>();
        if (!result.changed()) {
            lines.add("fand.yml unchanged.");
            return lines;
        }
        if (!result.hotApplied().isEmpty()) {
            lines.add("Hot-applied: " + String.join(", ", result.hotApplied()));
        }
        if (!result.requiresRestart().isEmpty()) {
            lines.add("Requires restart: " + String.join(", ", result.requiresRestart()));
        }
        return lines;
    }
}
