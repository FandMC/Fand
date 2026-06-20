package io.fand.api.event;

/**
 * Handle returned when a listener is registered with an {@link EventBus}. Calling
 * {@link #unregister()} removes only the listener this handle installed;
 * subsequent calls are no-ops. Closing this handle never removes a listener
 * registered afterwards, even for the same event type.
 */
public interface EventSubscription extends AutoCloseable {

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
