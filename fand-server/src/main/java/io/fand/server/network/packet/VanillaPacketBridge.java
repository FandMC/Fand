package io.fand.server.network.packet;

import io.fand.api.entity.Player;
import io.fand.api.packet.CustomPacket;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketProtocol;
import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import io.fand.api.player.PlayerProfile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.kyori.adventure.key.Key;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VanillaPacketBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaPacketBridge.class);
    private static final Set<String> OBJECT_METHODS = Set.of(
            "equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait");
    private static final PacketType[] NO_TYPES = new PacketType[0];
    private static final Map<String, PacketType[]> TYPES_BY_VANILLA_NAME = indexTypesByVanillaName();

    private final PacketRegistryImpl registry;
    private final PacketViewFactory viewFactory = new PacketViewFactory();
    private final ConcurrentMap<Class<?>, PacketShape> shapes = new ConcurrentHashMap<>();
    // Per-packet-class candidate types. Common packets (e.g. keep_alive) map the
    // same vanilla class to one PacketType per protocol phase, so this holds a
    // small array selected by protocol+direction at intercept time.
    private final ConcurrentMap<Class<?>, PacketType[]> typesByClass = new ConcurrentHashMap<>();

    VanillaPacketBridge(PacketRegistryImpl registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Nullable Packet<?> intercept(
            ConnectionProtocol vanillaProtocol,
            PacketDirection direction,
            Optional<? extends Player> player,
            Optional<PlayerProfile> profile,
            @Nullable SocketAddress remoteAddress,
            Packet<?> packet
    ) {
        var type = type(vanillaProtocol, direction, packet);
        if (type == null) {
            return packet;
        }

        var result = interceptVanilla(type, type.protocol(), direction, player, profile, remoteAddress, packet);
        if (result != null && direction == PacketDirection.SERVERBOUND) {
            dispatchCustomPayload(type.protocol(), direction, player, profile, remoteAddress, result);
        }
        return result;
    }

    private @Nullable PacketType type(ConnectionProtocol vanillaProtocol, PacketDirection direction, Packet<?> packet) {
        var candidates = typesByClass.computeIfAbsent(
                packet.getClass(),
                // Metadata stores nested packet classes in source form
                // (Outer.Inner); normalise the runtime binary name to match.
                packetClass -> TYPES_BY_VANILLA_NAME.getOrDefault(packetClass.getName().replace('$', '.'), NO_TYPES));
        for (var candidate : candidates) {
            // Vanilla and Fand protocol enums share the same phase ids, so name
            // equality is the protocol match; see the data generator contract.
            if (candidate.direction() == direction && candidate.protocol().name().equals(vanillaProtocol.name())) {
                return candidate;
            }
        }
        return null;
    }

    private static Map<String, PacketType[]> indexTypesByVanillaName() {
        var index = new java.util.HashMap<String, PacketType[]>();
        for (var type : PacketType.values()) {
            index.merge(type.vanillaClassName(), new PacketType[] {type}, (existing, single) -> {
                var grown = java.util.Arrays.copyOf(existing, existing.length + 1);
                grown[existing.length] = single[0];
                return grown;
            });
        }
        return Map.copyOf(index);
    }

    private @Nullable Packet<?> interceptVanilla(
            PacketType type,
            PacketProtocol protocol,
            PacketDirection direction,
            Optional<? extends Player> player,
            Optional<PlayerProfile> profile,
            @Nullable SocketAddress remoteAddress,
            Packet<?> packet
    ) {
        var registrations = registry.interceptors(type);
        if (registrations.isEmpty()) {
            return packet;
        }

        var shape = shape(packet.getClass());
        PacketView currentView;
        try {
            currentView = viewFactory.view(type, shape.read(packet));
        } catch (RuntimeException failure) {
            LOGGER.warn("Failed to read vanilla packet {}", packet.getClass().getName(), failure);
            return packet;
        }

        var context = new PacketContextImpl(protocol, direction, player, profile, remoteAddress);
        boolean replaced = false;
        for (var registration : registrations) {
            if (!registration.active()) {
                continue;
            }
            var controller = intercept(context, type, currentView, registration);
            if (controller == null) {
                continue;
            }
            if (controller.cancelled()) {
                return null;
            }
            if (controller.replaced()) {
                currentView = controller.view();
                replaced = true;
            }
        }

        if (!replaced) {
            return packet;
        }
        if (!shape.replaceable()) {
            LOGGER.warn("Ignoring replacement for unsupported packet {}", packet.getClass().getName());
            return packet;
        }
        try {
            return shape.rebuild(currentView);
        } catch (RuntimeException failure) {
            LOGGER.warn("Failed to rebuild vanilla packet {}", packet.getClass().getName(), failure);
            return packet;
        }
    }

    private void dispatchCustomPayload(
            PacketProtocol protocol,
            PacketDirection direction,
            Optional<? extends Player> player,
            Optional<PlayerProfile> profile,
            @Nullable SocketAddress remoteAddress,
            Packet<?> packet
    ) {
        if (!(packet instanceof ServerboundCustomPayloadPacket customPayload)
                || !(customPayload.payload() instanceof DiscardedPayload payload)) {
            return;
        }
        var channel = key(payload.id());
        var handler = registry.customHandler(protocol, channel).orElse(null);
        if (handler == null) {
            return;
        }
        var context = new PacketContextImpl(protocol, direction, player, profile, remoteAddress);
        try {
            handler.handle(context, new CustomPacket(channel, payload.payload()));
        } catch (RuntimeException failure) {
            LOGGER.warn("Custom packet handler failed for {}/{}", protocol.id(), channel.asString(), failure);
        }
    }

    private PacketShape shape(Class<?> packetClass) {
        return shapes.computeIfAbsent(packetClass, PacketShape::of);
    }

    private static PacketControllerImpl<? extends PacketView> intercept(
            io.fand.api.packet.PacketContext context,
            PacketType type,
            PacketView view,
            PacketRegistryImpl.InterceptorRegistration<? extends PacketView> registration
    ) {
        return interceptUnchecked(context, type, view, registration);
    }

    private static <T extends PacketView> PacketControllerImpl<T> interceptUnchecked(
            io.fand.api.packet.PacketContext context,
            PacketType type,
            PacketView view,
            PacketRegistryImpl.InterceptorRegistration<T> registration
    ) {
        var controller = new PacketControllerImpl<>(context, type, view.as(registration.viewType()));
        try {
            registration.interceptor().intercept(controller);
        } catch (RuntimeException failure) {
            LOGGER.warn("Packet interceptor failed for {}", type.asString(), failure);
            return null;
        }
        return controller;
    }

    private static Key key(Identifier identifier) {
        return Key.key(identifier.getNamespace(), identifier.getPath());
    }

    private static String decapitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        if (value.chars().allMatch(ch -> !Character.isLetter(ch) || Character.isUpperCase(ch))) {
            return value.toLowerCase(Locale.ROOT);
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private record PacketShape(
            List<FieldReader> readers,
            @Nullable Constructor<?> canonicalConstructor,
            List<String> recordComponentNames,
            @Nullable ClassRebuilder classRebuilder
    ) {

        static PacketShape of(Class<?> packetClass) {
            if (packetClass.isRecord()) {
                return recordShape(packetClass);
            }
            return classShape(packetClass);
        }

        boolean replaceable() {
            return canonicalConstructor != null || classRebuilder != null;
        }

        Map<String, Object> read(Packet<?> packet) {
            var fields = new LinkedHashMap<String, Object>();
            for (var reader : readers) {
                fields.putIfAbsent(reader.name(), reader.read(packet));
            }
            return fields;
        }

        Packet<?> rebuild(PacketView view) {
            if (canonicalConstructor != null) {
                var arguments = new Object[recordComponentNames.size()];
                for (int i = 0; i < recordComponentNames.size(); i++) {
                    arguments[i] = view.value(recordComponentNames.get(i));
                }
                try {
                    return (Packet<?>) canonicalConstructor.newInstance(arguments);
                } catch (ReflectiveOperationException failure) {
                    throw new IllegalStateException("Failed to call packet record constructor", failure);
                }
            }
            if (classRebuilder != null) {
                return classRebuilder.rebuild(view);
            }
            throw new IllegalStateException("Packet is not replaceable");
        }

        private static PacketShape recordShape(Class<?> packetClass) {
            var components = packetClass.getRecordComponents();
            var readers = new ArrayList<FieldReader>(components.length);
            var constructorTypes = new Class<?>[components.length];
            var names = new ArrayList<String>(components.length);
            for (int i = 0; i < components.length; i++) {
                var component = components[i];
                constructorTypes[i] = component.getType();
                names.add(component.getName());
                readers.add(new MethodFieldReader(component.getName(), component.getAccessor()));
            }
            try {
                var constructor = packetClass.getDeclaredConstructor(constructorTypes);
                constructor.setAccessible(true);
                return new PacketShape(List.copyOf(readers), constructor, List.copyOf(names), null);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Missing canonical record constructor for " + packetClass.getName(), failure);
            }
        }

        private static PacketShape classShape(Class<?> packetClass) {
            var readers = new LinkedHashMap<String, FieldReader>();
            var writers = new LinkedHashMap<String, FieldWriter>();
            for (Class<?> current = packetClass; current != null && current != Object.class; current = current.getSuperclass()) {
                collectGetterReaders(current, readers);
                collectPrivateFieldReaders(current, readers, writers);
            }
            var rebuilder = writers.isEmpty() ? null : new ClassRebuilder(packetClass, List.copyOf(writers.values()));
            return new PacketShape(List.copyOf(readers.values()), null, List.of(), rebuilder);
        }

        private static void collectGetterReaders(Class<?> packetClass, Map<String, FieldReader> readers) {
            for (var method : packetClass.getDeclaredMethods()) {
                if (!isGetter(method)) {
                    continue;
                }
                var methodName = method.getName();
                var fieldName = methodName.startsWith("get")
                        ? decapitalize(methodName.substring(3))
                        : decapitalize(methodName.substring(2));
                readers.putIfAbsent(fieldName, new MethodFieldReader(fieldName, method));
            }
        }

        private static void collectPrivateFieldReaders(
                Class<?> packetClass,
                Map<String, FieldReader> readers,
                Map<String, FieldWriter> writers
        ) {
            for (var field : packetClass.getDeclaredFields()) {
                var modifiers = field.getModifiers();
                if (!Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers) || field.isSynthetic()) {
                    continue;
                }
                readers.putIfAbsent(field.getName(), new ReflectiveFieldReader(field.getName(), field));
                writers.putIfAbsent(field.getName(), new ReflectiveFieldWriter(field.getName(), field));
            }
        }

        private static boolean isGetter(Method method) {
            var modifiers = method.getModifiers();
            if (!Modifier.isPublic(modifiers)
                    || Modifier.isStatic(modifiers)
                    || method.getParameterCount() != 0
                    || method.getReturnType() == Void.TYPE
                    || OBJECT_METHODS.contains(method.getName())) {
                return false;
            }
            var name = method.getName();
            return name.matches("get[A-Z].*") || name.matches("is[A-Z].*");
        }
    }

    private record ClassRebuilder(Class<?> packetClass, List<FieldWriter> writers) {

        Packet<?> rebuild(PacketView view) {
            var instance = allocate(packetClass);
            for (var writer : writers) {
                if (view.has(writer.name())) {
                    writer.write(instance, view.value(writer.name()));
                }
            }
            return (Packet<?>) instance;
        }
    }

    private interface FieldReader {

        String name();

        Object read(Packet<?> packet);
    }

    private interface FieldWriter {

        String name();

        void write(Object packet, Object value);
    }

    private record MethodFieldReader(String name, Method method) implements FieldReader {

        MethodFieldReader {
            method.setAccessible(true);
        }

        @Override
        public Object read(Packet<?> packet) {
            try {
                return method.invoke(packet);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Failed to read packet method " + method.getName(), failure);
            }
        }
    }

    private record ReflectiveFieldReader(String name, Field field) implements FieldReader {

        ReflectiveFieldReader {
            field.setAccessible(true);
        }

        @Override
        public Object read(Packet<?> packet) {
            try {
                return field.get(packet);
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException("Failed to read packet field " + field.getName(), failure);
            }
        }
    }

    private record ReflectiveFieldWriter(String name, Field field) implements FieldWriter {

        ReflectiveFieldWriter {
            field.setAccessible(true);
        }

        @Override
        public void write(Object packet, Object value) {
            try {
                field.set(packet, value);
            } catch (IllegalAccessException | IllegalArgumentException failure) {
                throw new IllegalStateException("Failed to write packet field " + field.getName(), failure);
            }
        }
    }

    private static Object allocate(Class<?> type) {
        return UnsafeAllocator.INSTANCE.allocate(type);
    }

    private enum UnsafeAllocator {
        INSTANCE;

        private final sun.misc.Unsafe unsafe = unsafe();

        Object allocate(Class<?> type) {
            try {
                return unsafe.allocateInstance(type);
            } catch (InstantiationException failure) {
                throw new IllegalStateException("Failed to allocate packet " + type.getName(), failure);
            }
        }

        private static sun.misc.Unsafe unsafe() {
            try {
                var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return (sun.misc.Unsafe) field.get(null);
            } catch (ReflectiveOperationException | InaccessibleObjectException failure) {
                throw new IllegalStateException("Cannot access Unsafe for packet replacement", failure);
            }
        }
    }
}
