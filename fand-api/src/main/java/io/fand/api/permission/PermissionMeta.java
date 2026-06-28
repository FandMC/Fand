package io.fand.api.permission;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolved chat and group metadata for a permission subject.
 */
public record PermissionMeta(
        Optional<String> prefix,
        Optional<String> suffix,
        Map<String, String> values,
        Optional<String> primaryGroup,
        List<String> groups
) {

    private static final PermissionMeta EMPTY = new PermissionMeta(
            Optional.empty(),
            Optional.empty(),
            Map.of(),
            Optional.empty(),
            List.of());

    public PermissionMeta {
        prefix = requireOptional(prefix, "prefix");
        suffix = requireOptional(suffix, "suffix");
        values = copyValues(values);
        primaryGroup = requireOptional(primaryGroup, "primaryGroup").map(PermissionGroup::normalizeName);
        groups = copyGroups(groups);
    }

    public static PermissionMeta empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> value(String key) {
        return Optional.ofNullable(values.get(PermissionContext.normalizeKey(key)));
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    private static Optional<String> requireOptional(Optional<String> value, String name) {
        return Objects.requireNonNull(value, name);
    }

    private static Map<String, String> copyValues(Map<String, String> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            return Map.of();
        }
        var copy = new LinkedHashMap<String, String>();
        for (var entry : values.entrySet()) {
            copy.put(PermissionContext.normalizeKey(entry.getKey()), Objects.requireNonNull(entry.getValue(), "meta value"));
        }
        return Map.copyOf(copy);
    }

    private static List<String> copyGroups(List<String> groups) {
        Objects.requireNonNull(groups, "groups");
        if (groups.isEmpty()) {
            return List.of();
        }
        return groups.stream()
                .map(PermissionGroup::normalizeName)
                .distinct()
                .toList();
    }

    public static final class Builder {

        private Optional<String> prefix = Optional.empty();
        private Optional<String> suffix = Optional.empty();
        private final LinkedHashMap<String, String> values = new LinkedHashMap<>();
        private Optional<String> primaryGroup = Optional.empty();
        private final LinkedHashMap<String, Boolean> groups = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(PermissionMeta meta) {
            this.prefix = meta.prefix;
            this.suffix = meta.suffix;
            this.values.putAll(meta.values);
            this.primaryGroup = meta.primaryGroup;
            for (var group : meta.groups) {
                this.groups.put(group, true);
            }
        }

        public Builder prefix(String prefix) {
            this.prefix = Optional.of(Objects.requireNonNull(prefix, "prefix"));
            return this;
        }

        public Builder prefix(Optional<String> prefix) {
            this.prefix = requireOptional(prefix, "prefix");
            return this;
        }

        public Builder suffix(String suffix) {
            this.suffix = Optional.of(Objects.requireNonNull(suffix, "suffix"));
            return this;
        }

        public Builder suffix(Optional<String> suffix) {
            this.suffix = requireOptional(suffix, "suffix");
            return this;
        }

        public Builder value(String key, String value) {
            values.put(PermissionContext.normalizeKey(key), Objects.requireNonNull(value, "value"));
            return this;
        }

        public Builder values(Map<String, String> values) {
            this.values.clear();
            this.values.putAll(copyValues(values));
            return this;
        }

        public Builder primaryGroup(String primaryGroup) {
            this.primaryGroup = Optional.of(PermissionGroup.normalizeName(primaryGroup));
            group(primaryGroup);
            return this;
        }

        public Builder primaryGroup(Optional<String> primaryGroup) {
            this.primaryGroup = requireOptional(primaryGroup, "primaryGroup").map(PermissionGroup::normalizeName);
            this.primaryGroup.ifPresent(this::group);
            return this;
        }

        public Builder group(String group) {
            groups.put(PermissionGroup.normalizeName(group), true);
            return this;
        }

        public Builder groups(List<String> groups) {
            this.groups.clear();
            for (var group : copyGroups(groups)) {
                this.groups.put(group, true);
            }
            return this;
        }

        public PermissionMeta build() {
            return new PermissionMeta(prefix, suffix, values, primaryGroup, List.copyOf(groups.keySet()));
        }
    }
}
