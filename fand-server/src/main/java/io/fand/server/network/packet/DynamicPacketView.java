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
 * <p>For Class-based packets (non-record), a snapshot record is used as the
 * source for field reads, and {@link ClassPacketCodec} rebuilds from the snapshot
 * on replace.
 *
 * <p>Typed views (e.g. {@code ClientboundSetEntityMotionView}) are dynamic
 * proxies that delegate their {@link PacketView} methods here; see
 * {@link PacketViewFactory}.
 */
final class DynamicPacketView implements PacketView {

    private final PacketType type;
    private final Packet<?> packet;
    private final PacketFieldModel model;
    private final Class<? extends PacketView> viewType;
    private final Map<String, Object> overrides;

    // For Class packets: snapshot record + codec
    private final @Nullable Object snapshot;
    private final @Nullable ClassPacketCodec<?, ?> classCodec;

    DynamicPacketView(PacketType type, Packet<?> packet, PacketFieldModel model, Class<? extends PacketView> viewType) {
        this(type, packet, model, viewType, Map.of(), null, null);
    }

    // Constructor for Class packets with snapshot
    DynamicPacketView(
            PacketType type,
            Packet<?> packet,
            PacketFieldModel model,
            Class<? extends PacketView> viewType,
            Object snapshot,
            ClassPacketCodec<?, ?> classCodec) {
        this(type, packet, model, viewType, Map.of(), snapshot, classCodec);
    }

    private DynamicPacketView(
            PacketType type,
            Packet<?> packet,
            PacketFieldModel model,
            Class<? extends PacketView> viewType,
            Map<String, Object> overrides) {
        this(type, packet, model, viewType, overrides, null, null);
    }

    private DynamicPacketView(
            PacketType type,
            Packet<?> packet,
            PacketFieldModel model,
            Class<? extends PacketView> viewType,
            Map<String, Object> overrides,
            @Nullable Object snapshot,
            @Nullable ClassPacketCodec<?, ?> classCodec) {
        this.type = type;
        this.packet = packet;
        this.model = model;
        this.viewType = viewType;
        this.overrides = overrides;
        this.snapshot = snapshot;
        this.classCodec = classCodec;
    }

    Class<? extends PacketView> viewType() {
        return viewType;
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
        Object value;
        if (overrides.containsKey(field)) {
            value = overrides.get(field);
        } else if (snapshot instanceof Object[]) {
            // For Object[] snapshot, read by index
            Object[] arr = (Object[]) snapshot;
            int index = indexOfField(field);
            value = PacketMarshalling.fromVanilla(arr[index]);
        } else if (snapshot != null) {
            // For record snapshot, read via model
            value = PacketMarshalling.fromVanilla(model.read(snapshot, field));
        } else {
            // No snapshot, read from packet directly
            value = PacketMarshalling.fromVanilla(model.read(packet, field));
        }

        if (value != null && valueType.isInstance(value)) {
            return Optional.of(valueType.cast(value));
        }
        return Optional.empty();
    }

    /** Gets the index of a field in the ordered field list. */
    private int indexOfField(String field) {
        int i = 0;
        for (String name : model.names()) {
            if (name.equals(field)) {
                return i;
            }
            i++;
        }
        throw new IllegalStateException("Field not found: " + field);
    }

    @Override
    public boolean canReplace() {
        return model.isRecord() || classCodec != null;
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
        return PacketViewFactory.wrap(
                new DynamicPacketView(type, packet, model, viewType, Map.copyOf(next), snapshot, classCodec));
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

        // Class packet: rebuild snapshot with overrides, then rebuild packet from snapshot
        if (classCodec != null && snapshot != null) {
            // Manually rebuild the snapshot record with overrides
            Object modifiedSnapshot = rebuildSnapshot(snapshot, overrides);
            // Rebuild the vanilla packet from the modified snapshot
            return classCodec.write(packet, modifiedSnapshot);
        }

        // Record packet: standard rebuild from the packet itself
        return model.rebuild(packet, overrides);
    }

    /** Rebuilds a snapshot record applying overrides. */
    private Object rebuildSnapshot(Object snapshot, Map<String, Object> overrides) {
        // Handle Object[] snapshot from GenericClassPacketCodec
        if (snapshot instanceof Object[]) {
            Object[] original = (Object[]) snapshot;
            Object[] modified = new Object[original.length];
            int i = 0;
            for (String fieldName : model.names()) {
                if (overrides.containsKey(fieldName)) {
                    modified[i] = PacketMarshalling.toVanilla(overrides.get(fieldName), model.fieldType(fieldName));
                } else {
                    modified[i] = original[i];
                }
                i++;
            }
            return modified;
        }

        // Handle record snapshot from dedicated ClassPacketCodec
        // Read all fields from the snapshot, applying overrides
        Map<String, Object> allFields = new LinkedHashMap<>();
        for (String fieldName : model.names()) {
            if (overrides.containsKey(fieldName)) {
                allFields.put(fieldName, overrides.get(fieldName));
            } else {
                allFields.put(fieldName, model.read(snapshot, fieldName));
            }
        }

        // Invoke the canonical constructor with the field values
        try {
            Class<?> snapshotClass = snapshot.getClass();
            java.lang.reflect.Constructor<?>[] constructors = snapshotClass.getDeclaredConstructors();
            // Find the canonical constructor (record constructors match field order)
            java.lang.reflect.Constructor<?> ctor = null;
            for (java.lang.reflect.Constructor<?> c : constructors) {
                if (c.getParameterCount() == allFields.size()) {
                    ctor = c;
                    break;
                }
            }
            if (ctor == null) {
                throw new IllegalStateException("No canonical constructor found for " + snapshotClass.getName());
            }

            // Build args in field declaration order
            Object[] args = new Object[allFields.size()];
            int i = 0;
            for (String fieldName : model.names()) {
                args[i++] = PacketMarshalling.toVanilla(allFields.get(fieldName), model.fieldType(fieldName));
            }

            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to rebuild snapshot " + snapshot.getClass().getName(), e);
        }
    }

    @Override
    public String toString() {
        return "PacketView[" + type + (overrides.isEmpty() ? "" : " modified=" + overrides.keySet()) + "]";
    }
}
