package io.fand.api.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (var method : current.getDeclaredMethods()) {
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
                handlers.add(new Handler(eventType, annotation.priority(), handle, method));
            }
        }
        return handlers;
    }

    private record Handler(
            Class<? extends Event> eventType,
            EventPriority priority,
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
            EventSubscription subscription = bus.subscribe((Class) eventType, priority, (EventListener) adapter);
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
