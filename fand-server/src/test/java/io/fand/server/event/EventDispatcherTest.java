package io.fand.server.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.event.Event;
import io.fand.api.event.EventDispatchException;
import io.fand.api.event.EventPriority;
import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.SubscriptionOptions;
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

    @Test
    void skipsAlreadyCancelledEventsWhenRequested() {
        List<String> calls = new ArrayList<>();
        bus.subscribe(CancelEvent.class, event -> {
            calls.add("cancel");
            event.setCancelled(true);
        });
        bus.subscribe(
                CancelEvent.class,
                new SubscriptionOptions(EventPriority.NORMAL, true),
                event -> calls.add("ignored")
        );
        bus.subscribe(CancelEvent.class, EventPriority.HIGH, event -> calls.add("observed"));

        bus.fire(new CancelEvent());

        assertThat(calls).containsExactly("cancel", "observed");
    }

    @Test
    void restoresAndReportsObserverMutationsBeforeInvokingTheNextObserver() {
        List<Boolean> observedCancellation = new ArrayList<>();
        var event = new CancelEvent();
        bus.subscribe(CancelEvent.class, EventPriority.OBSERVER, current -> current.setCancelled(true));
        bus.subscribe(CancelEvent.class, EventPriority.OBSERVER, current -> observedCancellation.add(current.cancelled()));

        assertThatThrownBy(() -> bus.fire(event))
                .isInstanceOfSatisfying(EventDispatchException.class, failure -> {
                    assertThat(failure.failures()).hasSize(1);
                    assertThat(failure.failures().getFirst())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("OBSERVER listener mutated")
                            .hasMessageContaining("cancelled");
                });
        assertThat(event.cancelled()).isFalse();
        assertThat(observedCancellation).containsExactly(false);
    }

    @Test
    void restoresMutableCollectionContentsChangedByObserver() {
        var event = new MutableCollectionEvent();
        bus.subscribe(MutableCollectionEvent.class, EventPriority.OBSERVER, current -> current.values().add("changed"));

        assertThatThrownBy(() -> bus.fire(event)).isInstanceOf(EventDispatchException.class);
        assertThat(event.values()).containsExactly("initial");
    }

    @Test
    void restoresAnImmutableCollectionReferenceReplacedByObserver() {
        var event = new ReplaceableCollectionEvent();
        bus.subscribe(ReplaceableCollectionEvent.class, EventPriority.OBSERVER, current -> current.setValues(List.of("changed")));

        assertThatThrownBy(() -> bus.fire(event)).isInstanceOf(EventDispatchException.class);
        assertThat(event.values()).containsExactly("initial");
    }

    private interface BaseEvent extends Event {
    }

    private record ChildEvent() implements BaseEvent {
    }

    private static final class CancelEvent implements Event, io.fand.api.event.Cancellable {
        private boolean cancelled;

        @Override
        public boolean cancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    private static final class MutableCollectionEvent implements Event {
        private final List<String> values = new ArrayList<>(List.of("initial"));

        List<String> values() {
            return values;
        }
    }

    private static final class ReplaceableCollectionEvent implements Event {
        private List<String> values = List.of("initial");

        List<String> values() {
            return values;
        }

        void setValues(List<String> values) {
            this.values = List.copyOf(values);
        }
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

    @Test
    void registerListenerUsesAnnotationOptionsAndSkipsOverriddenHandlers() {
        List<String> calls = new ArrayList<>();
        bus.registerListener(new ChildHandler(calls));

        var event = new CancelEvent();
        event.setCancelled(true);
        bus.fire(event);

        assertThat(calls).containsExactly("child", "second");
    }

    @Test
    void privateSameSignatureHandlersInParentAndChildAreBothRegistered() {
        List<String> calls = new ArrayList<>();
        bus.registerListener(new PrivateChildHandler(calls));

        bus.fire(new ChildEvent());

        assertThat(calls).containsExactly("child-private", "parent-private");
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

    private static class ParentHandler implements Listener {
        protected final List<String> calls;

        ParentHandler(List<String> calls) {
            this.calls = calls;
        }

        @Subscribe
        public void overridden(CancelEvent event) {
            calls.add("parent");
        }

        @Subscribe
        public void second(CancelEvent event) {
            calls.add("second");
        }

        @Subscribe(ignoreCancelled = true)
        public void skipped(CancelEvent event) {
            calls.add("skipped");
        }
    }

    private static final class ChildHandler extends ParentHandler {
        ChildHandler(List<String> calls) {
            super(calls);
        }

        @Override
        @Subscribe
        public void overridden(CancelEvent event) {
            calls.add("child");
        }
    }

    private static class PrivateParentHandler implements Listener {
        protected final List<String> calls;

        PrivateParentHandler(List<String> calls) {
            this.calls = calls;
        }

        @Subscribe
        private void sameName(ChildEvent event) {
            calls.add("parent-private");
        }
    }

    private static final class PrivateChildHandler extends PrivateParentHandler {
        PrivateChildHandler(List<String> calls) {
            super(calls);
        }

        @Subscribe
        private void sameName(ChildEvent event) {
            calls.add("child-private");
        }
    }

    @Test
    void propagatesErrorsWithoutWrappingThemAsListenerFailures() {
        var error = new AssertionError("fatal");
        bus.subscribe(ChildEvent.class, event -> {
            throw error;
        });

        assertThatThrownBy(() -> bus.fire(new ChildEvent())).isSameAs(error);
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
