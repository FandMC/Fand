package io.fand.api.permission;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only group view exposed by a permission provider.
 */
public record PermissionGroup(
        String name,
        Optional<String> displayName,
        List<String> parents,
        PermissionMeta meta
) {

    public PermissionGroup(String name) {
        this(name, Optional.empty(), List.of(), PermissionMeta.empty());
    }

    public PermissionGroup {
        name = normalizeName(name);
        displayName = Objects.requireNonNull(displayName, "displayName");
        parents = copyParents(parents);
        meta = Objects.requireNonNull(meta, "meta");
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    static String normalizeName(String name) {
        var normalized = Objects.requireNonNull(name, "name").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        return normalized;
    }

    private static List<String> copyParents(List<String> parents) {
        Objects.requireNonNull(parents, "parents");
        if (parents.isEmpty()) {
            return List.of();
        }
        return parents.stream()
                .map(PermissionGroup::normalizeName)
                .distinct()
                .toList();
    }

    public static final class Builder {

        private final String name;
        private Optional<String> displayName = Optional.empty();
        private final LinkedHashMap<String, Boolean> parents = new LinkedHashMap<>();
        private PermissionMeta meta = PermissionMeta.empty();

        private Builder(String name) {
            this.name = normalizeName(name);
        }

        public Builder displayName(String displayName) {
            this.displayName = Optional.of(Objects.requireNonNull(displayName, "displayName"));
            return this;
        }

        public Builder displayName(Optional<String> displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        public Builder parent(String parent) {
            parents.put(normalizeName(parent), true);
            return this;
        }

        public Builder parents(List<String> parents) {
            this.parents.clear();
            for (var parent : copyParents(parents)) {
                this.parents.put(parent, true);
            }
            return this;
        }

        public Builder meta(PermissionMeta meta) {
            this.meta = Objects.requireNonNull(meta, "meta");
            return this;
        }

        public PermissionGroup build() {
            return new PermissionGroup(name, displayName, List.copyOf(parents.keySet()), meta);
        }
    }
}
