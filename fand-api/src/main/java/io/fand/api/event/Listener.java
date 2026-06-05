package io.fand.api.event;

/**
 * Marker interface for objects that group multiple event handler methods. Methods
 * annotated with {@link Subscribe} on a {@code Listener} are bound when the
 * instance is passed to {@link EventBus#registerListener(Listener)}.
 *
 * <p>This is purely a tagging interface; no methods are required. The annotation
 * scan only looks at instances passed through the bus, so a class that does not
 * implement {@code Listener} cannot be registered by mistake.
 */
public interface Listener {
}
