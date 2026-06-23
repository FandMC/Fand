package io.fand.api.datapack;

import java.util.Objects;

/**
 * Immutable metadata used when creating a server data pack.
 */
public record DataPack(
        String id,
        String description,
        boolean enabled
) {

    public DataPack {
        id = normalizeId(id);
        description = Objects.requireNonNull(description, "description");
    }

    public static DataPack of(String id, String description) {
        return new DataPack(id, description, true);
    }

    private static String normalizeId(String id) {
        Objects.requireNonNull(id, "id");
        var normalized = id.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException("Data pack id must contain only lowercase letters, numbers, dot, underscore, or dash: " + id);
        }
        if (normalized.equals(".") || normalized.equals("..") || normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException("Data pack id must be a portable path segment: " + id);
        }
        return normalized;
    }
}
