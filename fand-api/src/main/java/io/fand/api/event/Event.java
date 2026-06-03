package io.fand.api.event;

/**
 * Marker interface for all events dispatched on the Fand {@link EventBus}.
 *
 * <p>Events are plain data carriers. Mutability and cancellability are opt-in via
 * {@link Cancellable}; any other behaviour belongs in the listener side.
 */
public interface Event {
}
