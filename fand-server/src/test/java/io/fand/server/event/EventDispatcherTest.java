package io.fand.server.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.event.Event;
import io.fand.api.event.EventDispatchException;
import io.fand.api.event.EventPriority;
import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
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

    @Test
    void registerListenerSubscribesAllAnnotatedMethods() {
        List<String> calls = new ArrayList<>();
        var listener = new MultiHandler(calls);

        var subscription = bus.registerListener(listener);

        bus.fire(new ChildEvent());
        assertThat(calls).containsExactly("base", "child-high");

        subscription.unregister();
        calls.clear();
        bus.fire(new ChildEvent());
        assertThat(calls).isEmpty();
    }

    @Test
    void registerListenerRejectsListenerWithoutAnnotatedMethods() {
        assertThatThrownBy(() -> bus.registerListener(new EmptyListener()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No @Subscribe methods");
    }

    @Test
    void registerListenerRejectsBadSignature() {
        assertThatThrownBy(() -> bus.registerListener(new BadSignatureListener()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MultiHandler implements Listener {
        private final List<String> calls;

        MultiHandler(List<String> calls) {
            this.calls = calls;
        }

        @Subscribe
        public void onBase(BaseEvent event) {
            calls.add("base");
        }

        @Subscribe(priority = EventPriority.HIGH)
        public void onChild(ChildEvent event) {
            calls.add("child-high");
        }
    }

    private static final class EmptyListener implements Listener {
    }

    private static final class BadSignatureListener implements Listener {
        @Subscribe
        public void notAnEvent(String wrong) {
        }
    }

    @Test
    void fireAsyncRunsListenersOnSuppliedExecutor() throws Exception {
        var executorThread = new java.util.concurrent.atomic.AtomicReference<Thread>();
        var caller = Thread.currentThread();
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            var t = new Thread(r, "async-dispatch-test");
            executorThread.set(t);
            return t;
        });
        try {
            List<Thread> seenThreads = java.util.Collections.synchronizedList(new ArrayList<>());
            bus.subscribe(ChildEvent.class, event -> seenThreads.add(Thread.currentThread()));
            bus.subscribe(ChildEvent.class, EventPriority.HIGH, event -> seenThreads.add(Thread.currentThread()));

            var event = new ChildEvent();
            var future = bus.fireAsync(event, executor);
            assertThat(future.get(5, java.util.concurrent.TimeUnit.SECONDS)).isSameAs(event);
            assertThat(seenThreads).hasSize(2);
            assertThat(seenThreads).allSatisfy(t -> assertThat(t).isNotEqualTo(caller));
            assertThat(seenThreads).allSatisfy(t -> assertThat(t.getName()).isEqualTo("async-dispatch-test"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void fireAsyncCompletesExceptionallyOnListenerFailure() {
        bus.subscribe(ChildEvent.class, event -> {
            throw new IllegalStateException("boom");
        });
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var future = bus.fireAsync(new ChildEvent(), executor);
            assertThatThrownBy(() -> future.get(5, java.util.concurrent.TimeUnit.SECONDS))
                    .isInstanceOf(java.util.concurrent.ExecutionException.class)
                    .hasCauseInstanceOf(EventDispatchException.class);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void hasListenersReportsConcreteAndSupertypeSubscriptions() {
        assertThat(bus.hasListeners(ChildEvent.class)).isFalse();

        var subscription = bus.subscribe(BaseEvent.class, event -> {});
        assertThat(bus.hasListeners(ChildEvent.class)).isTrue();
        assertThat(bus.hasListeners(BaseEvent.class)).isTrue();

        subscription.unregister();
        assertThat(bus.hasListeners(ChildEvent.class)).isFalse();
    }

    @Test
    void hasListenersStaysAccurateUnderConcurrentSubscribeUnregisterAndQuery() throws Exception {
        int threads = 8;
        int iterationsPerThread = 4_000;
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        try {
            var ready = new java.util.concurrent.CountDownLatch(threads);
            var go = new java.util.concurrent.CountDownLatch(1);
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>(threads);
            for (int t = 0; t < threads; t++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        var sub = bus.subscribe(ChildEvent.class, event -> {});
                        bus.hasListeners(ChildEvent.class);
                        bus.hasListeners(BaseEvent.class);
                        sub.unregister();
                    }
                    return null;
                }));
            }
            ready.await();
            go.countDown();
            for (var f : futures) {
                f.get(30, java.util.concurrent.TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(bus.hasListeners(ChildEvent.class)).isFalse();
        assertThat(bus.hasListeners(BaseEvent.class)).isFalse();

        var probe = bus.subscribe(ChildEvent.class, event -> {});
        assertThat(bus.hasListeners(ChildEvent.class)).isTrue();
        probe.unregister();
        assertThat(bus.hasListeners(ChildEvent.class)).isFalse();
    }
}
