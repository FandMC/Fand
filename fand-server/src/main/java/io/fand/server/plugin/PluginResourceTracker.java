package io.fand.server.plugin;

import io.fand.api.command.CommandRegistration;
import io.fand.api.customblock.CustomBlockRegistration;
import io.fand.api.customitem.CustomItemRegistration;
import io.fand.api.event.EventSubscription;
import io.fand.api.gui.GuiView;
import io.fand.api.packet.PacketRegistration;
import io.fand.api.recipe.RecipeRegistration;
import io.fand.api.scheduler.Task;
import io.fand.api.scoreboard.ScoreboardRegistration;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class PluginResourceTracker {

    private final Object lock = new Object();
    private final Set<TrackedSubscription> subscriptions = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedCommandRegistration> commandRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedRecipeRegistration> recipeRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedScoreboardRegistration> scoreboardRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedPacketRegistration> packetRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedCustomItemRegistration> customItemRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedCustomBlockRegistration> customBlockRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedGuiView> guiViews = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
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

    TrackedScoreboardRegistration track(ScoreboardRegistration delegate) {
        var tracked = new TrackedScoreboardRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                scoreboardRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedCustomItemRegistration track(CustomItemRegistration delegate) {
        var tracked = new TrackedCustomItemRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                customItemRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedCustomBlockRegistration track(CustomBlockRegistration delegate) {
        var tracked = new TrackedCustomBlockRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                customBlockRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedGuiView track(GuiView delegate) {
        var tracked = new TrackedGuiView(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                guiViews.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
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

    void release(TrackedScoreboardRegistration registration) {
        synchronized (lock) {
            scoreboardRegistrations.remove(registration);
        }
    }

    void release(TrackedPacketRegistration registration) {
        synchronized (lock) {
            packetRegistrations.remove(registration);
        }
    }

    void release(TrackedCustomItemRegistration registration) {
        synchronized (lock) {
            customItemRegistrations.remove(registration);
        }
    }

    void release(TrackedCustomBlockRegistration registration) {
        synchronized (lock) {
            customBlockRegistrations.remove(registration);
        }
    }

    void release(TrackedGuiView view) {
        synchronized (lock) {
            guiViews.remove(view);
        }
    }

    void close() {
        List<TrackedSubscription> subscriptionsToClose;
        List<TrackedCommandRegistration> commandRegistrationsToClose;
        List<TrackedRecipeRegistration> recipeRegistrationsToClose;
        List<TrackedScoreboardRegistration> scoreboardRegistrationsToClose;
        List<TrackedPacketRegistration> packetRegistrationsToClose;
        List<TrackedCustomItemRegistration> customItemRegistrationsToClose;
        List<TrackedCustomBlockRegistration> customBlockRegistrationsToClose;
        List<TrackedGuiView> guiViewsToClose;
        List<TrackedTask> tasksToClose;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            subscriptionsToClose = new ArrayList<>(subscriptions);
            commandRegistrationsToClose = new ArrayList<>(commandRegistrations);
            recipeRegistrationsToClose = new ArrayList<>(recipeRegistrations);
            scoreboardRegistrationsToClose = new ArrayList<>(scoreboardRegistrations);
            packetRegistrationsToClose = new ArrayList<>(packetRegistrations);
            customItemRegistrationsToClose = new ArrayList<>(customItemRegistrations);
            customBlockRegistrationsToClose = new ArrayList<>(customBlockRegistrations);
            guiViewsToClose = new ArrayList<>(guiViews);
            tasksToClose = new ArrayList<>(tasks);
            subscriptions.clear();
            commandRegistrations.clear();
            recipeRegistrations.clear();
            scoreboardRegistrations.clear();
            packetRegistrations.clear();
            customItemRegistrations.clear();
            customBlockRegistrations.clear();
            guiViews.clear();
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
        for (var registration : scoreboardRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : packetRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : customItemRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : customBlockRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var view : guiViewsToClose) {
            view.closeFromTracker();
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

    static final class TrackedScoreboardRegistration implements ScoreboardRegistration {

        private final PluginResourceTracker owner;
        private final ScoreboardRegistration delegate;
        private volatile boolean released;

        TrackedScoreboardRegistration(PluginResourceTracker owner, ScoreboardRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public String name() {
            return delegate.name();
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

    static final class TrackedCustomItemRegistration implements CustomItemRegistration {

        private final PluginResourceTracker owner;
        private final CustomItemRegistration delegate;
        private volatile boolean released;

        TrackedCustomItemRegistration(PluginResourceTracker owner, CustomItemRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key id() {
            return delegate.id();
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

    static final class TrackedCustomBlockRegistration implements CustomBlockRegistration {

        private final PluginResourceTracker owner;
        private final CustomBlockRegistration delegate;
        private volatile boolean released;

        TrackedCustomBlockRegistration(PluginResourceTracker owner, CustomBlockRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key id() {
            return delegate.id();
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

    static final class TrackedGuiView implements GuiView {

        private final PluginResourceTracker owner;
        private final GuiView delegate;
        private volatile boolean released;

        TrackedGuiView(PluginResourceTracker owner, GuiView delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public java.util.UUID id() {
            return delegate.id();
        }

        @Override
        public io.fand.api.entity.Player player() {
            return delegate.player();
        }

        @Override
        public io.fand.api.gui.Gui gui() {
            return delegate.gui();
        }

        @Override
        public io.fand.api.inventory.Inventory inventory() {
            return delegate.inventory();
        }

        @Override
        public boolean open() {
            return !released && delegate.open();
        }

        @Override
        public void reopen() {
            if (!released) {
                delegate.reopen();
            }
        }

        @Override
        public void close() {
            if (!released) {
                released = true;
                try {
                    delegate.close();
                } finally {
                    owner.release(this);
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }

        void releaseFromExternalClose() {
            if (!released) {
                released = true;
                owner.release(this);
            }
        }

        @Override
        public java.util.Optional<Object> state(String key) {
            return delegate.state(key);
        }

        @Override
        public void state(String key, Object value) {
            delegate.state(key, value);
        }

        @Override
        public void removeState(String key) {
            delegate.removeState(key);
        }
    }
}
