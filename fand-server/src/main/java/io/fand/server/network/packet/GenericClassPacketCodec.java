package io.fand.server.network.packet;

import net.minecraft.network.protocol.Packet;

/**
 * Generic codec for Class-based packets that automatically snapshots all fields
 * via reflection. Used as a fallback when no dedicated {@link ClassPacketCodec}
 * is registered for a Class packet.
 * <p>
 * This codec reads all {@code private final} fields into a map, then rebuilds
 * the packet by invoking the constructor that matches the field count and types.
 * It supports {@code replace()}, but with caveats:
 * <ul>
 *   <li>Constructor must be accessible and match field order</li>
 *   <li>Some packets with complex initialization may fail to rebuild</li>
 *   <li>For high-frequency packets, prefer dedicated snapshot records</li>
 * </ul>
 */
final class GenericClassPacketCodec {

    private GenericClassPacketCodec() {
    }

    static <P extends Packet<?>> ClassPacketCodec<P, Object> of(Class<P> packetClass) {
        return new ClassPacketCodec<>(packetClass, new GenericSnapshotOps<>(packetClass));
    }

    private static class GenericSnapshotOps<P extends Packet<?>> implements ClassPacketCodec.SnapshotOps<P, Object> {

        private final Class<P> packetClass;
        private final java.lang.reflect.Constructor<P> constructor;
        private final java.lang.reflect.Field[] fields;

        GenericSnapshotOps(Class<P> packetClass) {
            this.packetClass = packetClass;

            // Extract all private final fields
            var fieldList = new java.util.ArrayList<java.lang.reflect.Field>();
            for (java.lang.reflect.Field field : packetClass.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (java.lang.reflect.Modifier.isPrivate(mods) && java.lang.reflect.Modifier.isFinal(mods)) {
                    field.setAccessible(true);
                    fieldList.add(field);
                }
            }
            this.fields = fieldList.toArray(new java.lang.reflect.Field[0]);

            // Find a constructor matching field count
            this.constructor = findConstructor();
        }

        private java.lang.reflect.Constructor<P> findConstructor() {
            for (java.lang.reflect.Constructor<?> ctor : packetClass.getDeclaredConstructors()) {
                if (ctor.getParameterCount() == fields.length) {
                    ctor.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.lang.reflect.Constructor<P> typedCtor = (java.lang.reflect.Constructor<P>) ctor;
                    return typedCtor;
                }
            }
            throw new IllegalStateException("No matching constructor found for " + packetClass.getName());
        }

        @Override
        public Object capture(P packet) {
            // Snapshot fields into an Object array
            Object[] snapshot = new Object[fields.length];
            try {
                for (int i = 0; i < fields.length; i++) {
                    snapshot[i] = fields[i].get(packet);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot snapshot " + packetClass.getName(), e);
            }
            return snapshot;
        }

        @Override
        public Packet<?> rebuild(Object snapshot) {
            Object[] fieldValues = (Object[]) snapshot;
            try {
                return constructor.newInstance(fieldValues);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot rebuild " + packetClass.getName(), e);
            }
        }

        @Override
        public Class<?> snapshotClass() {
            return Object.class;
        }
    }
}
