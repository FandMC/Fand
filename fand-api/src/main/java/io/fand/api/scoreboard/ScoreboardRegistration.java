package io.fand.api.scoreboard;

/**
 * Handle returned by plugin-owned scoreboard registrations.
 *
 * <p>{@link #unregister()} removes only the objective/team this handle
 * installed. If the same name was re-registered after this handle was created,
 * closing this handle must not remove the newer registration; closing an
 * already-closed handle is a no-op.
 */
public interface ScoreboardRegistration extends AutoCloseable {

    String name();

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
