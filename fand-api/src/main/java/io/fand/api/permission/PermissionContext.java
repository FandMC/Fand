package io.fand.api.permission;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Context values used when resolving permission metadata.
 */
public record PermissionContext(Map<String, String> values) {

    private static final PermissionContext EMPTY = new PermissionContext(Map.of());

    public PermissionContext {
        values = copyValues(values);
    }

    public static PermissionContext empty() {
        return EMPTY;
    }

    public static PermissionContext of(String key, String value) {
        return new PermissionContext(Map.of(key, value));
    }

    public Optional<String> value(String key) {
        return Optional.ofNullable(values.get(normalizeKey(key)));
    }

    public boolean contains(String key) {
        return values.containsKey(normalizeKey(key));
    }

    public PermissionContext with(String key, String value) {
        var copy = new LinkedHashMap<>(values);
        copy.put(normalizeKey(key), Objects.requireNonNull(value, "value"));
        return new PermissionContext(copy);
    }

    public PermissionContext without(String key) {
        var copy = new LinkedHashMap<>(values);
        copy.remove(normalizeKey(key));
        return copy.isEmpty() ? empty() : new PermissionContext(copy);
    }

    static String normalizeKey(String key) {
        var normalized = Objects.requireNonNull(key, "key").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("key cannot be blank");
        }
        return normalized;
    }

    private static Map<String, String> copyValues(Map<String, String> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            return Map.of();
        }
        var copy = new LinkedHashMap<String, String>();
        for (var entry : values.entrySet()) {
            copy.put(normalizeKey(entry.getKey()), Objects.requireNonNull(entry.getValue(), "context value"));
        }
        return Map.copyOf(copy);
    }
}
