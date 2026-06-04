package io.fand.server.plugin;

import io.fand.api.event.EventSubscription;
import io.fand.api.scheduler.Task;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class PluginResourceTracker {

    private final Object lock = new Object();
    private final Set<TrackedSubscription> subscriptions = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedTask> tasks = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean closed;

    TrackedSubscription track(EventSubscription delegate) {
        var tracked = new TrackedSubscription(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                subscriptions.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedTask track(Task delegate) {
        var tracked = new TrackedTask(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                tasks.add(tracked);
            }
        }
        if (dispose) {
            tracked.cancelFromTracker();
        }
        return tracked;
    }

    void release(TrackedSubscription subscription) {
        synchronized (lock) {
            subscriptions.remove(subscription);
        }
    }

    void release(TrackedTask task) {
        synchronized (lock) {
            tasks.remove(task);
        }
    }

    void close() {
        List<TrackedSubscription> subscriptionsToClose;
        List<TrackedTask> tasksToClose;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            subscriptionsToClose = new ArrayList<>(subscriptions);
            tasksToClose = new ArrayList<>(tasks);
            subscriptions.clear();
            tasks.clear();
        }
        for (var subscription : subscriptionsToClose) {
            subscription.unregisterFromTracker();
        }
        for (var task : tasksToClose) {
            task.cancelFromTracker();
        }
    }

    static final class TrackedSubscription implements EventSubscription {

        private final PluginResourceTracker owner;
        private final EventSubscription delegate;
        private volatile boolean released;

        TrackedSubscription(PluginResourceTracker owner, EventSubscription delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedTask implements Task {

        private final PluginResourceTracker owner;
        private final Task delegate;
        private volatile boolean released;

        TrackedTask(PluginResourceTracker owner, Task delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public boolean cancelled() {
            return released || delegate.cancelled();
        }

        @Override
        public void cancel() {
            if (!released) {
                released = true;
                try {
                    delegate.cancel();
                } finally {
                    owner.release(this);
                }
            }
        }

        void cancelFromTracker() {
            if (!released) {
                released = true;
                delegate.cancel();
            }
        }
    }
}
