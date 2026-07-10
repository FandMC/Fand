package io.fand.api.resourcepack;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable metadata for a managed resource pack.
 */
public record ResourcePack(
        String id,
        String description,
        int packFormat
) {

    public ResourcePack {
        id = normalizeId(id);
        description = Objects.requireNonNull(description, "description");
        if (packFormat < 1) {
            throw new IllegalArgumentException("packFormat must be >= 1");
        }
    }

    public static ResourcePack of(String id, String description, int packFormat) {
        return new ResourcePack(id, description, packFormat);
    }

    public static String normalizeId(String id) {
        Objects.requireNonNull(id, "id");
        var normalized = id.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException("Resource pack id must contain only lowercase letters, numbers, dot, underscore, or dash: " + id);
        }
        if (normalized.equals(".") || normalized.equals("..") || normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException("Resource pack id must be a portable path segment: " + id);
        }
        return normalized;
    }

    public static String normalizeRelativePath(String value) {
        Objects.requireNonNull(value, "path");
        var normalized = value.replace('\\', '/');
        if (normalized.isBlank() || normalized.startsWith("/") || normalized.contains("://")) {
            throw new IllegalArgumentException("Resource pack path must be relative: " + value);
        }
        var path = Path.of(normalized).normalize();
        if (path.isAbsolute() || path.startsWith("..") || path.toString().equals("..")) {
            throw new IllegalArgumentException("Resource pack path escapes the pack root: " + value);
        }
        return path.toString().replace('\\', '/');
    }
}
