package io.fand.server.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.event.Event;
import io.fand.api.event.EventDispatchException;
import io.fand.api.event.EventPriority;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EventDispatcherTest {

    private final EventDispatcher bus = new EventDispatcher();

    @Test
    void dispatchesMatchingListenersByPriorityThenRegistrationOrder() {
        List<String> calls = new ArrayList<>();

        bus.subscribe(ChildEvent.class, EventPriority.HIGH, event -> calls.add("child-high"));
        bus.subscribe(Event.class, EventPriority.LOWEST, event -> calls.add("event-lowest"));
        bus.subscribe(BaseEvent.class, EventPriority.NORMAL, event -> calls.add("base-normal"));
        bus.subscribe(ChildEvent.class, EventPriority.NORMAL, event -> calls.add("child-normal"));

        var event = new ChildEvent();

        assertThat(bus.fire(event)).isSameAs(event);
        assertThat(calls).containsExactly("event-lowest", "base-normal", "child-normal", "child-high");
    }

    @Test
    void unregistersSubscriptionsIdempotently() {
        List<String> calls = new ArrayList<>();
        var subscription = bus.subscribe(ChildEvent.class, event -> calls.add("called"));

        assertThat(subscription.active()).isTrue();

        subscription.unregister();
        subscription.unregister();

        assertThat(subscription.active()).isFalse();
        bus.fire(new ChildEvent());
        assertThat(calls).isEmpty();
    }

    @Test
    void skipsSubscriptionUnregisteredBeforeItsTurn() {
        List<String> calls = new ArrayList<>();
        var second = bus.subscribe(ChildEvent.class, EventPriority.NORMAL, event -> calls.add("second"));
        bus.subscribe(ChildEvent.class, EventPriority.LOWEST, event -> {
            calls.add("first");
            second.unregister();
        });

        bus.fire(new ChildEvent());

        assertThat(calls).containsExactly("first");
    }

    @Test
    void rebuildsDispatchPlanWhenSubscriptionsChange() {
        List<String> calls = new ArrayList<>();

        var first = bus.subscribe(ChildEvent.class, event -> calls.add("first"));
        bus.fire(new ChildEvent());

        bus.subscribe(ChildEvent.class, event -> calls.add("second"));
        bus.fire(new ChildEvent());

        first.unregister();
        bus.fire(new ChildEvent());

        assertThat(calls).containsExactly("first", "first", "second", "second");
    }

    @Test
    void rebuildsDispatchPlanWhenNewListenerTypeAppears() {
        List<String> calls = new ArrayList<>();

        bus.subscribe(ChildEvent.class, event -> calls.add("child"));
        bus.fire(new ChildEvent());

        bus.subscribe(BaseEvent.class, event -> calls.add("base"));
        bus.fire(new ChildEvent());

        assertThat(calls).containsExactly("child", "child", "base");
    }

    @Test
    void invokesRemainingListenersAndReportsAllFailures() {
        List<String> calls = new ArrayList<>();

        bus.subscribe(ChildEvent.class, event -> {
            throw new IllegalStateException("first failure");
        });
        bus.subscribe(ChildEvent.class, event -> calls.add("after-first"));
        bus.subscribe(ChildEvent.class, event -> {
            throw new IOException("second failure");
        });

        assertThatThrownBy(() -> bus.fire(new ChildEvent()))
                .isInstanceOfSatisfying(EventDispatchException.class, failure -> {
                    assertThat(failure.eventType()).isEqualTo(ChildEvent.class);
                    assertThat(failure.failures()).hasSize(2);
                    assertThat(failure.failures().get(0)).isInstanceOf(IllegalStateException.class);
                    assertThat(failure.failures().get(1)).isInstanceOf(IOException.class);
                });
        assertThat(calls).containsExactly("after-first");
    }

    private interface BaseEvent extends Event {
    }

    private record ChildEvent() implements BaseEvent {
    }
}
