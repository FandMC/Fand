package io.fand.server.event;

import io.fand.api.event.Event;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventDispatchException;
import io.fand.api.event.EventListener;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class EventDispatcher implements EventBus {

    private static final int INACTIVE_COMPACTION_THRESHOLD = 256;
    private static final Registration<?>[] EMPTY_REGISTRATIONS = new Registration<?>[0];
    private static final BucketSnapshot[] EMPTY_SNAPSHOTS = new BucketSnapshot[0];
    private static final Comparator<Registration<?>> DISPATCH_ORDER = Comparator
            .comparing((Registration<?> registration) -> registration.priority)
            .thenComparingLong(registration -> registration.sequence);
    private static final Comparator<MergeCursor> MERGE_ORDER = Comparator.comparing(MergeCursor::registration, DISPATCH_ORDER);

    private final ConcurrentHashMap<Class<? extends Event>, ListenerBucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<? extends Event>, DispatchPlan> dispatchPlans = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<? extends Event>, AtomicLong> planGenerations = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public <E extends Event> EventSubscription subscribe(
            Class<E> type,
            EventPriority priority,
            EventListener<E> listener
    ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");

        var bucket = bucket(type);
        var registration = new Registration<>(this, bucket, priority, listener, sequence.getAndIncrement());
        bucket.add(registration);
        invalidatePlans(type);
        return registration;
    }

    @Override
    public <E extends Event> E fire(E event) {
        Objects.requireNonNull(event, "event");

        var plan = resolveDispatchPlan(event.getClass());
        var failures = invokeAll(plan, event);
        if (failures != null) {
            throw new EventDispatchException(event, failures);
        }
        return event;
    }

    @Override
    public <E extends Event> java.util.concurrent.CompletableFuture<E> fireAsync(
            E event,
            java.util.concurrent.Executor executor
    ) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(executor, "executor");

        var plan = resolveDispatchPlan(event.getClass());
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            var failures = invokeAll(plan, event);
            if (failures != null) {
                throw new EventDispatchException(event, failures);
            }
            return event;
        }, executor);
    }

    private DispatchPlan resolveDispatchPlan(Class<? extends Event> eventType) {
        var generation = planGeneration(eventType).get();
        var plan = dispatchPlans.get(eventType);
        if (plan == null || plan.generation != generation) {
            plan = rebuildDispatchPlan(eventType);
        }
        return plan;
    }

    private static List<Throwable> invokeAll(DispatchPlan plan, Event event) {
        List<Throwable> failures = null;
        for (var registration : plan.registrations) {
            var failure = registration.invoke(event);
            if (failure != null) {
                if (failures == null) {
                    failures = new ArrayList<>();
                }
                failures.add(failure);
            }
        }
        return failures;
    }

    private ListenerBucket bucket(Class<? extends Event> type) {
        var existing = buckets.get(type);
        if (existing != null) {
            return existing;
        }
        var created = new ListenerBucket(type);
        var raced = buckets.putIfAbsent(type, created);
        return raced != null ? raced : created;
    }

    private AtomicLong planGeneration(Class<? extends Event> eventType) {
        return planGenerations.computeIfAbsent(eventType, ignored -> new AtomicLong());
    }

    private void invalidatePlans(Class<? extends Event> listenerType) {
        for (var entry : planGenerations.entrySet()) {
            if (listenerType.isAssignableFrom(entry.getKey())) {
                entry.getValue().incrementAndGet();
            }
        }
    }

    private DispatchPlan rebuildDispatchPlan(Class<? extends Event> eventType) {
        var generation = planGeneration(eventType);
        while (true) {
            var targetGeneration = generation.get();
            var snapshots = snapshots(eventType);
            var registrations = mergeSnapshots(snapshots);
            if (generation.get() != targetGeneration) {
                continue;
            }
            var plan = new DispatchPlan(targetGeneration, registrations);
            dispatchPlans.put(eventType, plan);
            if (generation.get() == targetGeneration) {
                return plan;
            }
        }
    }

    private BucketSnapshot[] snapshots(Class<? extends Event> eventType) {
        List<BucketSnapshot> snapshots = null;
        for (var bucket : buckets.values()) {
            if (!bucket.type.isAssignableFrom(eventType)) {
                continue;
            }
            var snapshot = bucket.snapshot();
            if (snapshot.registrations.length == 0) {
                continue;
            }
            if (snapshots == null) {
                snapshots = new ArrayList<>();
            }
            snapshots.add(snapshot);
        }
        return snapshots == null ? EMPTY_SNAPSHOTS : snapshots.toArray(BucketSnapshot[]::new);
    }

    private Registration<?>[] mergeSnapshots(BucketSnapshot[] snapshots) {
        if (snapshots.length == 0) {
            return EMPTY_REGISTRATIONS;
        }
        if (snapshots.length == 1) {
            return snapshots[0].registrations;
        }

        var totalSize = 0;
        var queue = new PriorityQueue<MergeCursor>(snapshots.length, MERGE_ORDER);
        for (var snapshot : snapshots) {
            totalSize += snapshot.registrations.length;
            queue.add(new MergeCursor(snapshot.registrations));
        }

        var merged = new Registration<?>[totalSize];
        var index = 0;
        while (!queue.isEmpty()) {
            var cursor = queue.poll();
            merged[index++] = cursor.registration();
            if (cursor.advance()) {
                queue.add(cursor);
            }
        }
        return merged;
    }

    private static final class ListenerBucket {

        private final Class<? extends Event> type;
        private final Object lock = new Object();
        private final ArrayList<Registration<?>> registrations = new ArrayList<>();
        private final AtomicLong version = new AtomicLong();
        private final AtomicLong inactiveRegistrations = new AtomicLong();
        private volatile BucketSnapshot snapshot = new BucketSnapshot(0L, EMPTY_REGISTRATIONS);

        private ListenerBucket(Class<? extends Event> type) {
            this.type = type;
        }

        private void add(Registration<?> registration) {
            synchronized (lock) {
                registrations.add(registration);
                version.incrementAndGet();
            }
        }

        private void unregister() {
            inactiveRegistrations.incrementAndGet();
            version.incrementAndGet();
        }

        private BucketSnapshot snapshot() {
            var currentVersion = version.get();
            var currentSnapshot = snapshot;
            if (currentSnapshot.version == currentVersion) {
                return currentSnapshot;
            }
            synchronized (lock) {
                currentVersion = version.get();
                currentSnapshot = snapshot;
                if (currentSnapshot.version == currentVersion) {
                    return currentSnapshot;
                }
                compactInactiveIfNeeded();
                List<Registration<?>> active = null;
                for (var registration : registrations) {
                    if (!registration.active()) {
                        continue;
                    }
                    if (active == null) {
                        active = new ArrayList<>();
                    }
                    active.add(registration);
                }
                if (active != null) {
                    active.sort(DISPATCH_ORDER);
                }
                var updated = new BucketSnapshot(
                        currentVersion,
                        active == null ? EMPTY_REGISTRATIONS : active.toArray(Registration[]::new)
                );
                snapshot = updated;
                return updated;
            }
        }

        private void compactInactiveIfNeeded() {
            var inactive = inactiveRegistrations.get();
            if (inactive < INACTIVE_COMPACTION_THRESHOLD || inactive <= registrations.size() / 2L) {
                return;
            }
            var before = registrations.size();
            registrations.removeIf(registration -> !registration.active());
            var removed = before - registrations.size();
            if (removed > 0) {
                inactiveRegistrations.updateAndGet(current -> Math.max(0L, current - removed));
            }
        }
    }

    private record BucketSnapshot(long version, Registration<?>[] registrations) {
    }

    private record DispatchPlan(long generation, Registration<?>[] registrations) {
    }

    private static final class MergeCursor {

        private final Registration<?>[] registrations;
        private int index;

        private MergeCursor(Registration<?>[] registrations) {
            this.registrations = registrations;
        }

        private Registration<?> registration() {
            return registrations[index];
        }

        private boolean advance() {
            index++;
            return index < registrations.length;
        }
    }

    private static final class Registration<E extends Event> implements EventSubscription {

        private final EventDispatcher owner;
        private final ListenerBucket bucket;
        private final EventPriority priority;
        private final EventListener<E> listener;
        private final long sequence;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private Registration(
                EventDispatcher owner,
                ListenerBucket bucket,
                EventPriority priority,
                EventListener<E> listener,
                long sequence
        ) {
            this.owner = owner;
            this.bucket = bucket;
            this.priority = priority;
            this.listener = listener;
            this.sequence = sequence;
        }

        @Override
        public boolean active() {
            return active.get();
        }

        @Override
        public void unregister() {
            if (active.compareAndSet(true, false)) {
                bucket.unregister();
                owner.invalidatePlans(bucket.type);
            }
        }

        @SuppressWarnings("unchecked")
        private Throwable invoke(Event event) {
            if (!active()) {
                return null;
            }
            try {
                listener.on((E) event);
                return null;
            } catch (Exception failure) {
                return failure;
            }
        }
    }
}
