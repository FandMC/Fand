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

    /**
     * Registers every {@link Subscribe @Subscribe}-annotated method on
     * {@code listener}. The returned subscription unregisters all of them
     * atomically; individual methods cannot be cancelled separately.
     */
    default EventSubscription registerListener(Listener listener) {
        return ListenerBinder.bind(this, listener);
    }

    /** Fires {@code event} synchronously to all matching listeners. */
    <E extends Event> E fire(E event);

    /**
     * Fires {@code event} asynchronously on {@code executor}. Listeners are
     * invoked sequentially in dispatch order on a thread the executor supplies;
     * the returned future completes with the (possibly mutated) event once
     * every listener has run, or completes exceptionally with an
     * {@link EventDispatchException} if any listener threw.
     */
    <E extends Event> java.util.concurrent.CompletableFuture<E> fireAsync(E event, java.util.concurrent.Executor executor);
}
