package io.fand.api.event;

import java.util.Objects;

/**
 * Options controlling how an event listener is invoked.
 *
 * @param priority listener invocation priority
 * @param ignoreCancelled whether already-cancelled events should be skipped
 */
public record SubscriptionOptions(EventPriority priority, boolean ignoreCancelled) {

    public static final SubscriptionOptions DEFAULT = new SubscriptionOptions(EventPriority.NORMAL, false);

    public SubscriptionOptions {
        Objects.requireNonNull(priority, "priority");
    }

    public static SubscriptionOptions priority(EventPriority priority) {
        return new SubscriptionOptions(priority, false);
    }
}
