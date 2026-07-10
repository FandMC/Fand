package io.fand.api.datapack;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A file stored inside a managed data pack.
 */
public record DataPackFile(String packId, String path, long size) {

    public DataPackFile {
        packId = Objects.requireNonNull(packId, "packId");
        path = normalizeRelativePath(path);
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
    }

    public static String normalizeRelativePath(String value) {
        Objects.requireNonNull(value, "path");
        var normalized = value.replace('\\', '/');
        if (normalized.isBlank() || normalized.startsWith("/") || normalized.contains("://")) {
            throw new IllegalArgumentException("Data pack path must be relative: " + value);
        }
        var path = Path.of(normalized).normalize();
        if (path.isAbsolute() || path.startsWith("..") || path.toString().equals("..")) {
            throw new IllegalArgumentException("Data pack path escapes the pack root: " + value);
        }
        return path.toString().replace('\\', '/');
    }
}
