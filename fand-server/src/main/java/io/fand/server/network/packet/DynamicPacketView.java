package io.fand.server.network.packet;

import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.Nullable;

/**
 * Reflective {@link PacketView} backed by a live vanilla packet. Reads real
 * fields (marshalled to API types) and, for record packets, rebuilds the packet
 * with modified fields via {@link #with}.
 *
 * <p>Instances are immutable: {@link #with} returns a new view sharing the
 * original packet plus an accumulated override map. {@link #rebuild} applies the
 * overrides to produce a fresh vanilla packet; non-overridden fields (including
 * opaque ones) are carried over untouched.
 *
 * <p>Typed views (e.g. {@code ClientboundSetEntityMotionView}) are dynamic
 * proxies that delegate their {@link PacketView} methods here; see
 * {@link PacketViewFactory}.
 */
final class DynamicPacketView implements PacketView {

    private final PacketType type;
    private final Packet<?> packet;
    private final PacketFieldModel model;
    private final Map<String, Object> overrides;

    DynamicPacketView(PacketType type, Packet<?> packet, PacketFieldModel model) {
        this(type, packet, model, Map.of());
    }

    private DynamicPacketView(PacketType type, Packet<?> packet, PacketFieldModel model, Map<String, Object> overrides) {
        this.type = type;
        this.packet = packet;
        this.model = model;
        this.overrides = overrides;
    }

    @Override
    public PacketType type() {
        return type;
    }

    @Override
    public Set<String> fieldNames() {
        return model.names();
    }

    @Override
    public boolean has(String field) {
        return model.has(field);
    }

    @Override
    public <T> Optional<T> get(String field, Class<T> valueType) {
        if (!model.has(field)) {
            return Optional.empty();
        }
        Object value = overrides.containsKey(field)
                ? overrides.get(field)
                : PacketMarshalling.fromVanilla(model.read(packet, field));
        if (value != null && valueType.isInstance(value)) {
            return Optional.of(valueType.cast(value));
        }
        return Optional.empty();
    }

    @Override
    public boolean canReplace() {
        return model.isRecord();
    }

    @Override
    public PacketView with(String field, Object value) {
        if (!canReplace()) {
            throw new UnsupportedOperationException(type + " packets cannot be replaced (read-only)");
        }
        if (!model.has(field)) {
            throw new IllegalArgumentException("Unknown field '" + field + "' on " + type);
        }
        Map<String, Object> next = new LinkedHashMap<>(overrides);
        next.put(field, value);
        return new DynamicPacketView(type, packet, model, Map.copyOf(next));
    }

    /** True if {@link #with} has been applied at least once. */
    boolean isModified() {
        return !overrides.isEmpty();
    }

    /** Rebuilds the vanilla packet applying the accumulated overrides. */
    @Nullable Packet<?> rebuild() {
        if (overrides.isEmpty()) {
            return packet;
        }
        return model.rebuild(packet, overrides);
    }

    @Override
    public String toString() {
        return "PacketView[" + type + (overrides.isEmpty() ? "" : " modified=" + overrides.keySet()) + "]";
    }
}
