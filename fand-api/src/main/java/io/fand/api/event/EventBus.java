package io.fand.api.event;

/**
 * Central dispatcher for events. Implementations are required to be thread-safe.
 *
 * <p>The bus does not impose a threading model on listeners: a listener registered
 * here runs on whichever thread fired the event. Components that need a specific
 * thread should hop via the scheduler inside the listener.
 */
public interface EventBus {

    /** Registers a listener at {@link EventPriority#NORMAL}. */
    default <E extends Event> EventSubscription subscribe(Class<E> type, EventListener<E> listener) {
        return subscribe(type, EventPriority.NORMAL, listener);
    }

    <E extends Event> EventSubscription subscribe(Class<E> type, EventPriority priority, EventListener<E> listener);

    /** Fires {@code event} synchronously to all matching listeners. */
    <E extends Event> E fire(E event);
}
