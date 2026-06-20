package io.fand.server.plugin;

import io.fand.api.entity.Player;
import io.fand.api.tablist.TabListEntry;
import io.fand.api.tablist.TabListRegistration;
import io.fand.api.tablist.TabListService;
import java.util.HashSet;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PluginTabListService implements TabListService {

    private final TabListService delegate;
    private final PluginResourceTracker tracker;

    public PluginTabListService(TabListService delegate, PluginResourceTracker tracker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    @Override
    public Collection<? extends TabListRegistration> entries(Player viewer) {
        return delegate.entries(viewer);
    }

    @Override
    public Optional<? extends TabListRegistration> entry(Player viewer, UUID entryId) {
        return delegate.entry(viewer, entryId);
    }

    @Override
    public boolean visible(Player viewer, Player target) {
        return delegate.visible(viewer, target);
    }

    @Override
    public void setVisible(Player viewer, Player target, boolean visible) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(target, "target");
        delegate.setVisible(viewer, target, visible);
        if (visible) {
            tracker.restoreTabListVisibility(viewer.uniqueId(), target.uniqueId());
        } else {
            tracker.trackTabListVisibility(
                    viewer.uniqueId(),
                    target.uniqueId(),
                    () -> delegate.setVisible(viewer, target, true));
        }
    }

    @Override
    public void showOnly(Player viewer, Collection<? extends Player> visibleTargets) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(visibleTargets, "visibleTargets");
        Set<Player> candidates = new HashSet<>();
        candidates.addAll(viewer.world().players());
        candidates.addAll(visibleTargets);
        delegate.showOnly(viewer, visibleTargets);
        for (var target : visibleTargets) {
            Objects.requireNonNull(target, "visibleTargets cannot contain null");
        }
        for (var target : candidates) {
            if (target.uniqueId().equals(viewer.uniqueId()) || delegate.visible(viewer, target)) {
                tracker.restoreTabListVisibility(viewer.uniqueId(), target.uniqueId());
            } else {
                tracker.trackTabListVisibility(
                        viewer.uniqueId(),
                        target.uniqueId(),
                        () -> delegate.setVisible(viewer, target, true));
            }
        }
    }

    @Override
    public TabListRegistration add(Player viewer, TabListEntry entry) {
        return tracker.track(delegate.add(viewer, entry));
    }

    @Override
    public boolean remove(Player viewer, UUID entryId) {
        return delegate.remove(viewer, entryId);
    }

    @Override
    public void removeAll(Player viewer) {
        delegate.removeAll(viewer);
    }
}
