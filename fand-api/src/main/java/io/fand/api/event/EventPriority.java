package io.fand.api.event;

/**
 * Listener invocation order. Lower-priority listeners run first; observers run last
 * and must not mutate the event.
 */
public enum EventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    OBSERVER
}
