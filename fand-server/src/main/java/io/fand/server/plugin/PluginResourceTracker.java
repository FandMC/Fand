package io.fand.server.plugin;

import io.fand.api.command.CommandRegistration;
import io.fand.api.event.EventSubscription;
import io.fand.api.packet.PacketRegistration;
import io.fand.api.recipe.RecipeRegistration;
import io.fand.api.scheduler.Task;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class PluginResourceTracker {

    private final Object lock = new Object();
    private final Set<TrackedSubscription> subscriptions = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedCommandRegistration> commandRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedRecipeRegistration> recipeRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedPacketRegistration> packetRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
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

    TrackedCommandRegistration track(CommandRegistration delegate) {
        var tracked = new TrackedCommandRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                commandRegistrations.add(tracked);
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

    TrackedRecipeRegistration track(RecipeRegistration delegate) {
        var tracked = new TrackedRecipeRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                recipeRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedPacketRegistration track(PacketRegistration delegate) {
        var tracked = new TrackedPacketRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                packetRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    void release(TrackedSubscription subscription) {
        synchronized (lock) {
            subscriptions.remove(subscription);
        }
    }

    void release(TrackedCommandRegistration registration) {
        synchronized (lock) {
            commandRegistrations.remove(registration);
        }
    }

    void release(TrackedTask task) {
        synchronized (lock) {
            tasks.remove(task);
        }
    }

    void release(TrackedRecipeRegistration registration) {
        synchronized (lock) {
            recipeRegistrations.remove(registration);
        }
    }

    void release(TrackedPacketRegistration registration) {
        synchronized (lock) {
            packetRegistrations.remove(registration);
        }
    }

    void close() {
        List<TrackedSubscription> subscriptionsToClose;
        List<TrackedCommandRegistration> commandRegistrationsToClose;
        List<TrackedRecipeRegistration> recipeRegistrationsToClose;
        List<TrackedPacketRegistration> packetRegistrationsToClose;
        List<TrackedTask> tasksToClose;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            subscriptionsToClose = new ArrayList<>(subscriptions);
            commandRegistrationsToClose = new ArrayList<>(commandRegistrations);
            recipeRegistrationsToClose = new ArrayList<>(recipeRegistrations);
            packetRegistrationsToClose = new ArrayList<>(packetRegistrations);
            tasksToClose = new ArrayList<>(tasks);
            subscriptions.clear();
            commandRegistrations.clear();
            recipeRegistrations.clear();
            packetRegistrations.clear();
            tasks.clear();
        }
        for (var subscription : subscriptionsToClose) {
            subscription.unregisterFromTracker();
        }
        for (var registration : commandRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : recipeRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : packetRegistrationsToClose) {
            registration.unregisterFromTracker();
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

    static final class TrackedCommandRegistration implements CommandRegistration {

        private final PluginResourceTracker owner;
        private final CommandRegistration delegate;
        private volatile boolean released;

        TrackedCommandRegistration(PluginResourceTracker owner, CommandRegistration delegate) {
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

    static final class TrackedRecipeRegistration implements RecipeRegistration {

        private final PluginResourceTracker owner;
        private final RecipeRegistration delegate;
        private volatile boolean released;

        TrackedRecipeRegistration(PluginResourceTracker owner, RecipeRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
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

    static final class TrackedPacketRegistration implements PacketRegistration {

        private final PluginResourceTracker owner;
        private final PacketRegistration delegate;
        private volatile boolean released;

        TrackedPacketRegistration(PluginResourceTracker owner, PacketRegistration delegate) {
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
}
