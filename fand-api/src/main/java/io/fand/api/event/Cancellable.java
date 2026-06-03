package io.fand.api.event;

/**
 * Mixin for events whose dispatch can be aborted by a listener.
 *
 * <p>A cancelled event is still delivered to subsequent listeners so they may
 * inspect or un-cancel it; the event source decides what cancellation means.
 */
public interface Cancellable {

    boolean cancelled();

    void setCancelled(boolean cancelled);
}
