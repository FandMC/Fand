package io.fand.api.event;

/**
 * Functional listener invoked when an {@link Event} of type {@code E} is fired.
 */
@FunctionalInterface
public interface EventListener<E extends Event> {

    void on(E event) throws Exception;
}
