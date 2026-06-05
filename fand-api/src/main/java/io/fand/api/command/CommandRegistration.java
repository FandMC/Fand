package io.fand.api.command;

/**
 * Handle returned when a command is registered.
 */
public interface CommandRegistration extends AutoCloseable {

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
