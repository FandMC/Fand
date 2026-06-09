package io.fand.api.scoreboard;

/**
 * Handle returned by plugin-owned scoreboard registrations.
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
