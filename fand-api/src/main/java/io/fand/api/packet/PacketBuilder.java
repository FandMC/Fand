package io.fand.api.packet;

import io.fand.api.entity.Player;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Mutable builder for a {@link PacketView} backed by public packet metadata.
 */
public final class PacketBuilder {

    private final PacketRegistry registry;
    private final PacketType type;
    private final LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

    PacketBuilder(PacketRegistry registry, PacketType type) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.type = Objects.requireNonNull(type, "type");
    }

    public PacketType type() {
        return type;
    }

    public PacketBuilder field(String name, @Nullable Object value) {
        fields.put(Objects.requireNonNull(name, "name"), value);
        return this;
    }

    public PacketBuilder fields(Map<String, ?> values) {
        Objects.requireNonNull(values, "values");
        values.forEach(this::field);
        return this;
    }

    public Map<String, Object> fields() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public PacketView build() {
        return registry.packet(type, fields);
    }

    public <T extends PacketView> T build(Class<T> viewType) {
        return build().as(viewType);
    }

    public boolean send(Player viewer) {
        return registry.sender().send(viewer, build());
    }

    public int send(Collection<? extends Player> viewers) {
        return registry.sender().send(viewers, build());
    }
}
