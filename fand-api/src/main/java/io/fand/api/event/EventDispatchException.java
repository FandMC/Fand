package io.fand.api.event;

import java.util.List;

public final class EventDispatchException extends RuntimeException {

    private final Class<? extends Event> eventType;
    private final List<Throwable> failures;

    public EventDispatchException(Event event, List<Throwable> failures) {
        super(message(event, failures), failures.isEmpty() ? null : failures.getFirst());
        this.eventType = event.getClass();
        this.failures = List.copyOf(failures);
        for (int i = 1; i < failures.size(); i++) {
            addSuppressed(failures.get(i));
        }
    }

    public Class<? extends Event> eventType() {
        return eventType;
    }

    public List<Throwable> failures() {
        return failures;
    }

    private static String message(Event event, List<Throwable> failures) {
        return "Event " + event.getClass().getName() + " failed in " + failures.size() + " listener(s)";
    }
}
