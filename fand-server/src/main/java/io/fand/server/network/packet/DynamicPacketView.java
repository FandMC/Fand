package io.fand.server.network.packet;

import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public final class DynamicPacketView implements PacketView {

    private static final Map<Class<?>, Class<?>> BOXED_TYPES = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            char.class, Character.class);

    private final PacketType packetType;
    private final Map<String, Object> fields;

    public DynamicPacketView(PacketType packetType, Map<String, ?> fields) {
        this.packetType = Objects.requireNonNull(packetType, "packetType");
        this.fields = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(fields, "fields")));
    }

    @Override
    public PacketType packetType() {
        return packetType;
    }

    @Override
    public Set<String> fields() {
        return fields.keySet();
    }

    @Override
    public boolean has(String field) {
        return fields.containsKey(field);
    }

    @Override
    public Object value(String field) {
        if (!fields.containsKey(field)) {
            throw new NoSuchElementException("Packet " + packetType.asString() + " has no field '" + field + "'");
        }
        return fields.get(field);
    }

    @Override
    public <T> T value(String field, Class<T> type) {
        Objects.requireNonNull(type, "type");
        var value = value(field);
        if (value == null) {
            if (type.isPrimitive()) {
                throw new ClassCastException("Cannot read null packet field '" + field + "' as " + type.getName());
            }
            return null;
        }
        var boxedType = boxed(type);
        if (!boxedType.isInstance(value)) {
            throw new ClassCastException(
                    "Packet field '" + field + "' is " + value.getClass().getName() + ", not " + boxedType.getName());
        }
        return (T) value;
    }

    @Override
    public PacketView with(String field, Object value) {
        if (!fields.containsKey(field)) {
            throw new NoSuchElementException("Packet " + packetType.asString() + " has no field '" + field + "'");
        }
        var replacement = new LinkedHashMap<>(fields);
        replacement.put(field, value);
        return new DynamicPacketView(packetType, replacement);
    }

    @Override
    public <T extends PacketView> T as(Class<T> viewType) {
        Objects.requireNonNull(viewType, "viewType");
        if (viewType == PacketView.class || viewType == DynamicPacketView.class) {
            return viewType.cast(this);
        }
        if (!viewType.isInterface() || !PacketView.class.isAssignableFrom(viewType)) {
            throw new IllegalArgumentException("Packet view type must be an interface extending PacketView: " + viewType.getName());
        }
        if (!viewType.isAssignableFrom(packetType.viewType())) {
            throw new IllegalArgumentException(
                    "View type " + viewType.getName() + " is not compatible with " + packetType.viewType().getName());
        }
        var proxy = Proxy.newProxyInstance(
                viewType.getClassLoader(),
                new Class<?>[] { viewType },
                new TypedViewInvocationHandler(this));
        return viewType.cast(proxy);
    }

    @Override
    public String toString() {
        return "DynamicPacketView[type=" + packetType.asString() + ", fields=" + fields + "]";
    }

    private static Class<?> boxed(Class<?> type) {
        return type.isPrimitive() ? BOXED_TYPES.get(type) : type;
    }

    private record TypedViewInvocationHandler(DynamicPacketView delegate) implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return invokeObject(proxy, method, args);
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            return method.invoke(delegate, args);
        }

        private Object invokeObject(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "toString" -> delegate.toString();
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.toString());
            };
        }
    }
}
