package io.fand.server.network.packet;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.Packet;

/**
 * Cached reflective description of a vanilla packet's fields, shared by all
 * {@link DynamicPacketView} instances for that packet class.
 *
 * <p>Record packets are read through their component accessors and rebuilt via
 * the canonical constructor — these support {@code replace}. Non-record packets
 * are read through their declared instance fields and are read-only (they
 * cannot be reconstructed generically).
 */
final class PacketFieldModel {

    private static final Map<Class<?>, PacketFieldModel> CACHE = new ConcurrentHashMap<>();

    private final boolean record;
    private final Set<String> names;
    private final Map<String, Class<?>> types;
    private final Map<String, Method> accessors;
    private final Map<String, Field> fields;
    private final MethodHandle canonicalConstructor;
    private final List<String> orderedNames;

    private PacketFieldModel(
            boolean record,
            Map<String, Class<?>> types,
            Map<String, Method> accessors,
            Map<String, Field> fields,
            MethodHandle canonicalConstructor) {
        this.record = record;
        this.types = types;
        this.accessors = accessors;
        this.fields = fields;
        this.canonicalConstructor = canonicalConstructor;
        this.orderedNames = List.copyOf(types.keySet());
        this.names = Collections.unmodifiableSet(new LinkedHashSet<>(types.keySet()));
    }

    static PacketFieldModel of(Class<?> packetClass) {
        return CACHE.computeIfAbsent(packetClass, PacketFieldModel::build);
    }

    private static PacketFieldModel build(Class<?> packetClass) {
        if (packetClass.isRecord()) {
            return buildRecord(packetClass);
        }
        return buildClass(packetClass);
    }

    private static PacketFieldModel buildRecord(Class<?> packetClass) {
        RecordComponent[] components = packetClass.getRecordComponents();
        Map<String, Class<?>> types = new LinkedHashMap<>();
        Map<String, Method> accessors = new LinkedHashMap<>();
        Class<?>[] paramTypes = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            types.put(component.getName(), component.getType());
            Method accessor = component.getAccessor();
            accessor.setAccessible(true);
            accessors.put(component.getName(), accessor);
            paramTypes[i] = component.getType();
        }
        MethodHandle ctor;
        try {
            var constructor = packetClass.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            ctor = MethodHandles.lookup().unreflectConstructor(constructor);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("No canonical constructor for record " + packetClass.getName(), failure);
        }
        return new PacketFieldModel(true, types, accessors, Map.of(), ctor);
    }

    private static PacketFieldModel buildClass(Class<?> packetClass) {
        Map<String, Class<?>> types = new LinkedHashMap<>();
        Map<String, Field> fields = new LinkedHashMap<>();
        for (Class<?> c = packetClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || field.isSynthetic()) {
                    continue;
                }
                if (fields.containsKey(field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                fields.put(field.getName(), field);
                types.put(field.getName(), field.getType());
            }
        }
        return new PacketFieldModel(false, types, Map.of(), fields, null);
    }

    boolean isRecord() {
        return record;
    }

    Set<String> names() {
        return names;
    }

    boolean has(String name) {
        return types.containsKey(name);
    }

    Object read(Object packet, String name) {
        try {
            if (record) {
                Method accessor = accessors.get(name);
                if (accessor == null) {
                    throw new NoSuchElementException(
                            "No accessor for field '" + name + "' on " + packet.getClass().getSimpleName()
                                    + " (available fields: " + String.join(", ", names) + ")"
                    );
                }
                return accessor.invoke(packet);
            }
            Field field = fields.get(name);
            if (field == null) {
                throw new NoSuchElementException(
                        "No field '" + name + "' on " + packet.getClass().getSimpleName()
                                + " (available fields: " + String.join(", ", names) + ")"
                );
            }
            return field.get(packet);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot read field '" + name + "' of " + packet.getClass().getName(), failure);
        }
    }

    /**
     * Rebuilds a record packet, substituting the API values in
     * {@code apiOverrides} (keyed by field name) for the originals.
     */
    Packet<?> rebuild(Object original, Map<String, Object> apiOverrides) {
        if (!record) {
            throw new UnsupportedOperationException("Packet " + original.getClass().getName() + " cannot be rebuilt");
        }
        List<Object> args = new ArrayList<>(orderedNames.size());
        for (String name : orderedNames) {
            if (apiOverrides.containsKey(name)) {
                args.add(PacketMarshalling.toVanilla(apiOverrides.get(name), types.get(name)));
            } else {
                args.add(read(original, name));
            }
        }
        try {
            return (Packet<?>) canonicalConstructor.invokeWithArguments(args);
        } catch (Throwable failure) {
            throw new IllegalStateException("Cannot rebuild " + original.getClass().getName(), failure);
        }
    }

    Class<?> fieldType(String name) {
        return types.get(name);
    }
}
