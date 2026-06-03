package io.fand.api.scheduler;

/**
 * Handle to a scheduled job. Cancellation is best-effort; an in-flight task may
 * complete after {@link #cancel()} returns.
 */
public interface Task {

    boolean cancelled();

    void cancel();
}
