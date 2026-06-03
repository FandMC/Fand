package io.fand.api.event;

/**
 * Handle returned when a listener is registered with an {@link EventBus}. Calling
 * {@link #unregister()} removes the listener; subsequent calls are no-ops.
 */
public interface EventSubscription extends AutoCloseable {

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
