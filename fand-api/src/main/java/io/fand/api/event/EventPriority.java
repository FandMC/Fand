package io.fand.api.event;

/**
 * Listener invocation order. Lower-priority listeners run first; observers run last.
 * The server restores mutations attempted by an observer and reports them as a
 * listener failure.
 */
public enum EventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    OBSERVER
}
