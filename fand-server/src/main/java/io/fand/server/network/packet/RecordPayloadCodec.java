package io.fand.server.network.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serializes a custom packet payload record to and from a flat byte array.
 *
 * <p>Supports the scalar component types a plugin channel realistically needs:
 * {@code int}, {@code long}, {@code float}, {@code double}, {@code boolean},
 * {@code String}, {@code UUID}, and {@code byte[]}. A record carrying any other
 * component type is rejected at encode/decode time rather than silently
 * mis-serialized.
 */
final class RecordPayloadCodec {

    private static final Map<Class<?>, MethodHandle> CONSTRUCTORS = new ConcurrentHashMap<>();

    private RecordPayloadCodec() {
    }

    static byte[] encode(Record payload) {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            for (RecordComponent component : payload.getClass().getRecordComponents()) {
                writeComponent(out, component.getType(), readComponent(payload, component));
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to encode payload " + payload.getClass().getName(), failure);
        }
        return bytes.toByteArray();
    }

    static <P extends Record> P decode(Class<P> payloadType, byte[] data) {
        RecordComponent[] components = payloadType.getRecordComponents();
        Object[] args = new Object[components.length];
        try (var in = new DataInputStream(new ByteArrayInputStream(data))) {
            for (int i = 0; i < components.length; i++) {
                args[i] = readValue(in, components[i].getType());
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to decode payload " + payloadType.getName(), failure);
        }
        try {
            return payloadType.cast(constructor(payloadType).invokeWithArguments(args));
        } catch (Throwable failure) {
            throw new IllegalStateException("Cannot construct payload " + payloadType.getName(), failure);
        }
    }

    private static Object readComponent(Record payload, RecordComponent component) {
        try {
            return component.getAccessor().invoke(payload);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot read component " + component.getName(), failure);
        }
    }

    private static void writeComponent(DataOutputStream out, Class<?> type, Object value) throws IOException {
        if (type == int.class) {
            out.writeInt((int) value);
        } else if (type == long.class) {
            out.writeLong((long) value);
        } else if (type == float.class) {
            out.writeFloat((float) value);
        } else if (type == double.class) {
            out.writeDouble((double) value);
        } else if (type == boolean.class) {
            out.writeBoolean((boolean) value);
        } else if (type == String.class) {
            out.writeUTF((String) value);
        } else if (type == UUID.class) {
            var uuid = (UUID) value;
            out.writeLong(uuid.getMostSignificantBits());
            out.writeLong(uuid.getLeastSignificantBits());
        } else if (type == byte[].class) {
            var array = (byte[]) value;
            out.writeInt(array.length);
            out.write(array);
        } else {
            throw new IllegalArgumentException("Unsupported payload component type: " + type.getName());
        }
    }

    private static Object readValue(DataInputStream in, Class<?> type) throws IOException {
        if (type == int.class) {
            return in.readInt();
        }
        if (type == long.class) {
            return in.readLong();
        }
        if (type == float.class) {
            return in.readFloat();
        }
        if (type == double.class) {
            return in.readDouble();
        }
        if (type == boolean.class) {
            return in.readBoolean();
        }
        if (type == String.class) {
            return in.readUTF();
        }
        if (type == UUID.class) {
            long most = in.readLong();
            long least = in.readLong();
            return new UUID(most, least);
        }
        if (type == byte[].class) {
            var array = new byte[in.readInt()];
            in.readFully(array);
            return array;
        }
        throw new IllegalArgumentException("Unsupported payload component type: " + type.getName());
    }

    private static MethodHandle constructor(Class<?> payloadType) {
        return CONSTRUCTORS.computeIfAbsent(payloadType, type -> {
            RecordComponent[] components = type.getRecordComponents();
            Class<?>[] paramTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                paramTypes[i] = components[i].getType();
            }
            try {
                return MethodHandles.lookup().unreflectConstructor(type.getDeclaredConstructor(paramTypes));
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("No canonical constructor for " + type.getName(), failure);
            }
        });
    }
}
