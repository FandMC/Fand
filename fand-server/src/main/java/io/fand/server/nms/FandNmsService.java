package io.fand.server.nms;

import io.fand.api.nms.NmsAccess;
import io.fand.api.nms.NmsHook;
import io.fand.api.nms.NmsHookContext;
import io.fand.api.nms.NmsHookRegistration;
import io.fand.api.nms.NmsHookResult;
import io.fand.api.nms.NmsHookService;
import io.fand.api.nms.NmsService;
import io.fand.api.service.ServicePriority;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FandNmsService implements NmsService {

    private final FandNmsAccess access;
    private final HookRegistry hooks = new HookRegistry();

    public FandNmsService(Supplier<Object> server) {
        this.access = new FandNmsAccess(server);
    }

    @Override
    public NmsAccess access() {
        return access;
    }

    @Override
    public NmsHookService hooks() {
        return hooks;
    }

    public NmsHookRegistration registerHook(Key hook, NmsHook handler, ServicePriority priority, String owner) {
        return hooks.register(hook, handler, priority, owner);
    }

    public NmsHookResult dispatch(Key hook, Object instance, Object... arguments) {
        return hooks.dispatch(hook, instance, List.of(arguments));
    }

    private static final class FandNmsAccess implements NmsAccess {

        private final Supplier<Object> server;

        private FandNmsAccess(Supplier<Object> server) {
            this.server = Objects.requireNonNull(server, "server");
        }

        @Override
        public Object server() {
            var current = server.get();
            if (current == null) {
                throw new IllegalStateException("Minecraft server is not attached yet");
            }
            return current;
        }

        @Override
        public Object handle(Object apiObject) {
            return handleOrEmpty(apiObject).orElseThrow(() ->
                    new IllegalArgumentException("Object does not expose an NMS handle: " + apiObject.getClass().getName()));
        }

        @Override
        public Optional<Object> handleOrEmpty(Object apiObject) {
            Objects.requireNonNull(apiObject, "apiObject");
            if (apiObject instanceof net.minecraft.server.MinecraftServer
                    || apiObject instanceof net.minecraft.world.entity.Entity
                    || apiObject instanceof net.minecraft.server.level.ServerLevel) {
                return Optional.of(apiObject);
            }
            Method method = findZeroArgMethod(apiObject.getClass(), "handle").orElse(null);
            if (method == null) {
                return Optional.empty();
            }
            try {
                method.setAccessible(true);
                return Optional.ofNullable(method.invoke(apiObject));
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to read handle from " + apiObject.getClass().getName(), ex);
            }
        }

        @Override
        public Class<?> type(String className) {
            Objects.requireNonNull(className, "className");
            try {
                return Class.forName(className, false, getClass().getClassLoader());
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException("Unknown NMS class: " + className, ex);
            }
        }

        @Override
        public <T> Class<? extends T> type(String className, Class<T> expectedType) {
            return type(className).asSubclass(Objects.requireNonNull(expectedType, "expectedType"));
        }

        @Override
        public Object get(Object target, String fieldName) {
            Objects.requireNonNull(target, "target");
            try {
                Field field = findField(targetClass(target), fieldName);
                return field.get(Modifier.isStatic(field.getModifiers()) ? null : target);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to read field " + fieldName + " from " + targetClass(target).getName(), ex);
            }
        }

        @Override
        public <T> T get(Object target, String fieldName, Class<T> expectedType) {
            return Objects.requireNonNull(expectedType, "expectedType").cast(get(target, fieldName));
        }

        @Override
        public void set(Object target, String fieldName, Object value) {
            Objects.requireNonNull(target, "target");
            try {
                Field field = findField(targetClass(target), fieldName);
                field.set(Modifier.isStatic(field.getModifiers()) ? null : target, value);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to write field " + fieldName + " on " + targetClass(target).getName(), ex);
            }
        }

        @Override
        public Object call(Object target, String methodName, Object... args) {
            Objects.requireNonNull(target, "target");
            Object[] actualArgs = args == null ? new Object[0] : args;
            try {
                Method method = findMethod(targetClass(target), methodName, actualArgs);
                return method.invoke(Modifier.isStatic(method.getModifiers()) ? null : target, actualArgs);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to call method " + methodName + " on " + targetClass(target).getName(), ex);
            }
        }

        @Override
        public <T> T call(Object target, String methodName, Class<T> expectedType, Object... args) {
            return Objects.requireNonNull(expectedType, "expectedType").cast(call(target, methodName, args));
        }

        @Override
        public Object construct(String className, Object... args) {
            Object[] actualArgs = args == null ? new Object[0] : args;
            try {
                Constructor<?> constructor = findConstructor(type(className), actualArgs);
                return constructor.newInstance(actualArgs);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to construct " + className, ex);
            }
        }

        @Override
        public <T> T construct(String className, Class<T> expectedType, Object... args) {
            return Objects.requireNonNull(expectedType, "expectedType").cast(construct(className, args));
        }

        private static Class<?> targetClass(Object target) {
            return target instanceof Class<?> type ? type : target.getClass();
        }

        private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
            Objects.requireNonNull(fieldName, "fieldName");
            Class<?> current = type;
            while (current != null) {
                try {
                    Field field = current.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
            throw new NoSuchFieldException(type.getName() + "." + fieldName);
        }

        private static Optional<Method> findZeroArgMethod(Class<?> type, String methodName) {
            Class<?> current = type;
            while (current != null) {
                try {
                    Method method = current.getDeclaredMethod(methodName);
                    if (method.getParameterCount() == 0) {
                        return Optional.of(method);
                    }
                } catch (NoSuchMethodException ignored) {
                    current = current.getSuperclass();
                }
            }
            return Optional.empty();
        }

        private static Method findMethod(Class<?> type, String methodName, Object[] args) throws NoSuchMethodException {
            Objects.requireNonNull(methodName, "methodName");
            Class<?> current = type;
            while (current != null) {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getName().equals(methodName) && parametersMatch(method.getParameterTypes(), args)) {
                        method.setAccessible(true);
                        return method;
                    }
                }
                current = current.getSuperclass();
            }
            throw new NoSuchMethodException(type.getName() + "." + methodName + Arrays.toString(argumentTypes(args)));
        }

        private static Constructor<?> findConstructor(Class<?> type, Object[] args) throws NoSuchMethodException {
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                if (parametersMatch(constructor.getParameterTypes(), args)) {
                    constructor.setAccessible(true);
                    return constructor;
                }
            }
            throw new NoSuchMethodException(type.getName() + Arrays.toString(argumentTypes(args)));
        }

        private static boolean parametersMatch(Class<?>[] parameterTypes, Object[] args) {
            if (parameterTypes.length != args.length) {
                return false;
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                if (args[i] == null) {
                    continue;
                }
                if (!box(parameterTypes[i]).isAssignableFrom(args[i].getClass())) {
                    return false;
                }
            }
            return true;
        }

        private static Class<?>[] argumentTypes(Object[] args) {
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = args[i] == null ? Void.TYPE : args[i].getClass();
            }
            return types;
        }

        private static Class<?> box(Class<?> type) {
            if (!type.isPrimitive()) {
                return type;
            }
            if (type == int.class) return Integer.class;
            if (type == long.class) return Long.class;
            if (type == boolean.class) return Boolean.class;
            if (type == double.class) return Double.class;
            if (type == float.class) return Float.class;
            if (type == byte.class) return Byte.class;
            if (type == short.class) return Short.class;
            if (type == char.class) return Character.class;
            if (type == void.class) return Void.class;
            return type;
        }
    }

    private static final class HookRegistry implements NmsHookService {

        private static final Logger LOGGER = LoggerFactory.getLogger(HookRegistry.class);

        private final Object lock = new Object();
        private final LinkedHashMap<RegistrationId, Registration> registrations = new LinkedHashMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public NmsHookRegistration register(Key hook, NmsHook handler) {
            return register(hook, handler, ServicePriority.NORMAL, "server");
        }

        @Override
        public NmsHookRegistration register(Key hook, NmsHook handler, ServicePriority priority) {
            return register(hook, handler, priority, "server");
        }

        private NmsHookRegistration register(Key hook, NmsHook handler, ServicePriority priority, String owner) {
            var registration = new Registration(
                    this,
                    Objects.requireNonNull(hook, "hook"),
                    Objects.requireNonNull(handler, "handler"),
                    Objects.requireNonNull(owner, "owner"),
                    Objects.requireNonNull(priority, "priority"),
                    sequence.incrementAndGet());
            synchronized (lock) {
                registrations.put(new RegistrationId(registration.hook(), registration.sequence()), registration);
            }
            return registration;
        }

        @Override
        public List<NmsHookRegistration> hooks(Key hook) {
            Objects.requireNonNull(hook, "hook");
            synchronized (lock) {
                return ordered(hook).stream().map(registration -> (NmsHookRegistration) registration).toList();
            }
        }

        private NmsHookResult dispatch(Key hook, Object instance, List<Object> arguments) {
            var context = new HookContext(hook, instance, arguments);
            for (var registration : hooks(hook)) {
                try {
                    var result = Objects.requireNonNull(
                            registration.hookHandler().invoke(context),
                            "NMS hook returned null");
                    if (result.action() != NmsHookResult.Action.PASS) {
                        return result;
                    }
                } catch (Exception failure) {
                    LOGGER.warn("NMS hook {} from {} failed", registration.hook(), registration.owner(), failure);
                }
            }
            return NmsHookResult.pass();
        }

        private List<Registration> ordered(Key hook) {
            return registrations.values().stream()
                    .filter(Registration::active)
                    .filter(registration -> registration.hook().equals(hook))
                    .sorted(HookRegistry::compare)
                    .toList();
        }

        private static int compare(Registration left, Registration right) {
            int priority = Integer.compare(right.priority().ordinal(), left.priority().ordinal());
            if (priority != 0) {
                return priority;
            }
            return Long.compare(right.sequence(), left.sequence());
        }

        private void release(Registration registration) {
            synchronized (lock) {
                registrations.remove(new RegistrationId(registration.hook(), registration.sequence()), registration);
            }
        }

        private record RegistrationId(Key hook, long sequence) {
        }

        private record HookContext(Key hook, Object instance, List<Object> arguments) implements NmsHookContext {
            private HookContext {
                Objects.requireNonNull(hook, "hook");
                arguments = List.copyOf(arguments);
            }
        }

        private static final class Registration implements NmsHookRegistration {

            private final HookRegistry owner;
            private final Key hook;
            private final NmsHook handler;
            private final String providerOwner;
            private final ServicePriority priority;
            private final long sequence;
            private volatile boolean active = true;

            private Registration(
                    HookRegistry owner,
                    Key hook,
                    NmsHook handler,
                    String providerOwner,
                    ServicePriority priority,
                    long sequence
            ) {
                this.owner = owner;
                this.hook = hook;
                this.handler = handler;
                this.providerOwner = providerOwner;
                this.priority = priority;
                this.sequence = sequence;
            }

            @Override
            public Key hook() {
                return hook;
            }

            @Override
            public NmsHook hookHandler() {
                return handler;
            }

            @Override
            public String owner() {
                return providerOwner;
            }

            @Override
            public ServicePriority priority() {
                return priority;
            }

            @Override
            public boolean active() {
                return active;
            }

            @Override
            public void unregister() {
                if (active) {
                    active = false;
                    owner.release(this);
                }
            }

            private long sequence() {
                return sequence;
            }
        }
    }
}
