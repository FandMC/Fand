package io.fand.api.command;

/**
 * Handle returned when a command is registered.
 *
 * <p>{@link #unregister()} removes only the command this handle installed. If
 * the same label was re-registered after this handle was created, closing this
 * handle must not remove the newer registration; closing an already-closed
 * handle is a no-op.
 */
public interface CommandRegistration extends AutoCloseable {

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
