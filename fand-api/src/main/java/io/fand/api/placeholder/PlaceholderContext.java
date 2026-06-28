package io.fand.api.placeholder;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.world.World;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Viewer-relative context passed to placeholder providers.
 */
public record PlaceholderContext(
        @Nullable Player viewer,
        @Nullable Player target,
        @Nullable World world,
        @Nullable Entity entity,
        Map<String, Object> values
) {

    private static final PlaceholderContext EMPTY = new PlaceholderContext(null, null, null, null, Map.of());

    public PlaceholderContext {
        values = copyValues(values);
    }

    public static PlaceholderContext empty() {
        return EMPTY;
    }

    public static PlaceholderContext viewer(@Nullable Player viewer) {
        return builder().viewer(viewer).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<Player> viewerOptional() {
        return Optional.ofNullable(viewer);
    }

    public Optional<Player> targetOptional() {
        return Optional.ofNullable(target);
    }

    public Optional<World> worldOptional() {
        return Optional.ofNullable(world);
    }

    public Optional<Entity> entityOptional() {
        return Optional.ofNullable(entity);
    }

    public Optional<Object> value(String key) {
        return Optional.ofNullable(values.get(normalizeKey(key)));
    }

    public <T> Optional<T> value(String key, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return value(key).map(type::cast);
    }

    public PlaceholderContext withViewer(@Nullable Player viewer) {
        return new PlaceholderContext(viewer, target, world, entity, values);
    }

    public PlaceholderContext withTarget(@Nullable Player target) {
        return new PlaceholderContext(viewer, target, world, entity, values);
    }

    public PlaceholderContext withWorld(@Nullable World world) {
        return new PlaceholderContext(viewer, target, world, entity, values);
    }

    public PlaceholderContext withEntity(@Nullable Entity entity) {
        return new PlaceholderContext(viewer, target, world, entity, values);
    }

    public PlaceholderContext with(String key, Object value) {
        var copy = new LinkedHashMap<>(values);
        copy.put(normalizeKey(key), Objects.requireNonNull(value, "value"));
        return new PlaceholderContext(viewer, target, world, entity, copy);
    }

    public PlaceholderContext without(String key) {
        var copy = new LinkedHashMap<>(values);
        copy.remove(normalizeKey(key));
        return new PlaceholderContext(viewer, target, world, entity, copy);
    }

    private static Map<String, Object> copyValues(Map<String, Object> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            return Map.of();
        }
        var copy = new LinkedHashMap<String, Object>();
        for (var entry : values.entrySet()) {
            copy.put(normalizeKey(entry.getKey()), Objects.requireNonNull(entry.getValue(), "context value"));
        }
        return Map.copyOf(copy);
    }

    private static String normalizeKey(String key) {
        var normalized = Objects.requireNonNull(key, "key").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("key cannot be blank");
        }
        return normalized;
    }

    public static final class Builder {

        private @Nullable Player viewer;
        private @Nullable Player target;
        private @Nullable World world;
        private @Nullable Entity entity;
        private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder viewer(@Nullable Player viewer) {
            this.viewer = viewer;
            return this;
        }

        public Builder target(@Nullable Player target) {
            this.target = target;
            return this;
        }

        public Builder world(@Nullable World world) {
            this.world = world;
            return this;
        }

        public Builder entity(@Nullable Entity entity) {
            this.entity = entity;
            return this;
        }

        public Builder value(String key, Object value) {
            values.put(normalizeKey(key), Objects.requireNonNull(value, "value"));
            return this;
        }

        public Builder values(Map<String, Object> values) {
            Objects.requireNonNull(values, "values");
            values.forEach(this::value);
            return this;
        }

        public PlaceholderContext build() {
            return new PlaceholderContext(viewer, target, world, entity, values);
        }
    }
}
