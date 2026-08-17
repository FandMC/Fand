package io.fand.api.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reflective binder that turns {@link Subscribe @Subscribe} methods on a
 * {@link Listener} into individual {@link EventBus#subscribe} registrations.
 */
final class ListenerBinder {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private ListenerBinder() {
    }

    static EventSubscription bind(EventBus bus, Listener listener) {
        var handlers = handlersOn(listener.getClass());
        if (handlers.isEmpty()) {
            throw new IllegalArgumentException(
                    "No @Subscribe methods on " + listener.getClass().getName()
            );
        }
        var subscriptions = new ArrayList<EventSubscription>(handlers.size());
        try {
            for (var handler : handlers) {
                subscriptions.add(handler.subscribe(bus, listener));
            }
        } catch (RuntimeException failure) {
            for (var existing : subscriptions) {
                existing.unregister();
            }
            throw failure;
        }
        return new CompositeSubscription(subscriptions);
    }

    private static List<Handler> handlersOn(Class<?> type) {
        var handlers = new ArrayList<Handler>();
        var subclassMethods = new ArrayList<Method>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            var methods = current.getDeclaredMethods();
            java.util.Arrays.sort(methods, Comparator.comparing(ListenerBinder::stableMethodKey));
            for (var method : methods) {
                if (method.isBridge() || method.isSynthetic()) {
                    continue;
                }
                if (subclassMethods.stream().anyMatch(subclassMethod -> overrides(subclassMethod, method))) {
                    continue;
                }
                var annotation = method.getAnnotation(Subscribe.class);
                if (annotation == null) {
                    continue;
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("@Subscribe method must not be static: " + method);
                }
                if (method.getReturnType() != void.class) {
                    throw new IllegalArgumentException("@Subscribe method must return void: " + method);
                }
                if (method.getParameterCount() != 1) {
                    throw new IllegalArgumentException("@Subscribe method must take exactly one parameter: " + method);
                }
                var paramType = method.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(paramType)) {
                    throw new IllegalArgumentException("@Subscribe parameter must extend Event: " + method);
                }
                method.setAccessible(true);
                MethodHandle handle;
                try {
                    handle = LOOKUP.unreflect(method);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException("Failed to access @Subscribe method: " + method, ex);
                }
                @SuppressWarnings("unchecked")
                var eventType = (Class<? extends Event>) paramType;
                handlers.add(new Handler(
                        eventType,
                        new SubscriptionOptions(annotation.priority(), annotation.ignoreCancelled()),
                        handle,
                        method
                ));
            }
            for (var method : methods) {
                if (!method.isBridge() && !method.isSynthetic()) {
                    subclassMethods.add(method);
                }
            }
        }
        handlers.sort(Comparator
                .comparing((Handler handler) -> handler.eventType().getName())
                .thenComparing(handler -> stableMethodKey(handler.method())));
        return handlers;
    }

    private static String stableMethodKey(Method method) {
        var key = new StringBuilder(method.getName()).append('(');
        for (var parameterType : method.getParameterTypes()) {
            key.append(parameterType.getName()).append(';');
        }
        return key.append(')').toString();
    }

    private static boolean overrides(Method subclassMethod, Method superclassMethod) {
        if (!subclassMethod.getName().equals(superclassMethod.getName())
                || !java.util.Arrays.equals(subclassMethod.getParameterTypes(), superclassMethod.getParameterTypes())) {
            return false;
        }
        int subclassModifiers = subclassMethod.getModifiers();
        int superclassModifiers = superclassMethod.getModifiers();
        if (Modifier.isPrivate(subclassModifiers)
                || Modifier.isPrivate(superclassModifiers)
                || Modifier.isStatic(subclassModifiers)
                || Modifier.isStatic(superclassModifiers)) {
            return false;
        }
        if (!superclassMethod.getDeclaringClass().isAssignableFrom(subclassMethod.getDeclaringClass())) {
            return false;
        }
        boolean packagePrivate = !Modifier.isPublic(superclassModifiers)
                && !Modifier.isProtected(superclassModifiers);
        return !packagePrivate
                || superclassMethod.getDeclaringClass().getPackageName()
                .equals(subclassMethod.getDeclaringClass().getPackageName());
    }

    private record Handler(
            Class<? extends Event> eventType,
            SubscriptionOptions options,
            MethodHandle handle,
            Method method
    ) {

        EventSubscription subscribe(EventBus bus, Listener listener) {
            EventListener<Event> adapter = event -> {
                try {
                    handle.invoke(listener, event);
                } catch (Throwable failure) {
                    if (failure instanceof Error error) {
                        throw error;
                    }
                    if (failure instanceof Exception exception) {
                        throw exception;
                    }
                    throw new RuntimeException(failure);
                }
            };
            @SuppressWarnings({"unchecked", "rawtypes"})
            EventSubscription subscription = bus.subscribe((Class) eventType, options, (EventListener) adapter);
            return subscription;
        }
    }

    private static final class CompositeSubscription implements EventSubscription {

        private final List<EventSubscription> children;
        private final AtomicBoolean active = new AtomicBoolean(true);

        CompositeSubscription(List<EventSubscription> children) {
            this.children = List.copyOf(children);
        }

        @Override
        public boolean active() {
            if (!active.get()) {
                return false;
            }
            for (var child : children) {
                if (child.active()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void unregister() {
            if (active.compareAndSet(true, false)) {
                for (var child : children) {
                    child.unregister();
                }
            }
        }
    }
}
