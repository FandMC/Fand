package io.fand.server.scoreboard;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ScoreboardNames {

    private static final Pattern LOCAL = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*");
    private static final Pattern QUALIFIED = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*(?::[a-z0-9]+(?:[._-][a-z0-9]+)*)?");
    private static final int MAX_OBJECTIVE_NAME_LENGTH = 16;
    private static final int MAX_TEAM_NAME_LENGTH = 16;

    private ScoreboardNames() {
    }

    public static String normalizeGlobal(String name, int maxLength) {
        var normalized = Objects.requireNonNull(name, "name").trim().toLowerCase(Locale.ROOT);
        if (!QUALIFIED.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid scoreboard name: " + name);
        }
        requireLength(normalized, maxLength);
        return normalized;
    }

    public static String normalizePluginLocal(String namespace, String name, int maxLength) {
        var local = Objects.requireNonNull(name, "name").trim().toLowerCase(Locale.ROOT);
        if (local.contains(":")) {
            local = local.substring(local.indexOf(':') + 1);
        }
        if (!LOCAL.matcher(local).matches()) {
            throw new IllegalArgumentException("Invalid plugin scoreboard name: " + name);
        }
        var scoped = namespace + ":" + local;
        requireLength(scoped, maxLength);
        return scoped;
    }

    public static String normalizeObjective(String name) {
        return normalizeGlobal(name, MAX_OBJECTIVE_NAME_LENGTH);
    }

    public static String normalizeTeam(String name) {
        return normalizeGlobal(name, MAX_TEAM_NAME_LENGTH);
    }

    public static String normalizePluginObjective(String namespace, String name) {
        return normalizePluginLocal(namespace, name, MAX_OBJECTIVE_NAME_LENGTH);
    }

    public static String normalizePluginTeam(String namespace, String name) {
        return normalizePluginLocal(namespace, name, MAX_TEAM_NAME_LENGTH);
    }

    private static void requireLength(String name, int maxLength) {
        if (name.length() > maxLength) {
            throw new IllegalArgumentException("Scoreboard name '" + name + "' exceeds " + maxLength + " characters");
        }
    }
}
