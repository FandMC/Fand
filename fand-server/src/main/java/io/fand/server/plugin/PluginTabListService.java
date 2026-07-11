package io.fand.server.plugin;

import io.fand.api.entity.Player;
import io.fand.api.tablist.TabListEntry;
import io.fand.api.tablist.TabListRegistration;
import io.fand.api.tablist.TabListService;
import io.fand.server.tablist.RealPlayerTabListAccess;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PluginTabListService implements TabListService {

    private final TabListService delegate;
    private final PluginResourceTracker tracker;
    private final PluginTabListVisibilityRegistry visibilityRegistry;

    public PluginTabListService(
            TabListService delegate,
            PluginResourceTracker tracker,
            PluginTabListVisibilityRegistry visibilityRegistry
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.visibilityRegistry = Objects.requireNonNull(visibilityRegistry, "visibilityRegistry");
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
        if (visible) {
            tracker.restoreTabListVisibility(viewer.uniqueId(), target.uniqueId());
        } else {
            hide(
                    viewer.uniqueId(),
                    target.uniqueId(),
                    () -> delegate.setVisible(viewer, target, false),
                    () -> delegate.setVisible(viewer, target, true));
        }
    }

    @Override
    public void showOnly(Player viewer, Collection<? extends Player> visibleTargets) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(visibleTargets, "visibleTargets");
        for (var target : visibleTargets) {
            Objects.requireNonNull(target, "visibleTargets cannot contain null");
        }
        RealPlayerTabListAccess realPlayers = delegate instanceof RealPlayerTabListAccess access ? access : null;
        if (realPlayers == null) {
            showOnlyWithPlayerCandidates(viewer, visibleTargets);
            return;
        }

        Set<UUID> candidateIds = new HashSet<>();
        var viewerId = viewer.uniqueId();
        candidateIds.addAll(realPlayers.showOnlyCandidateIds(viewer, visibleTargets));
        for (var target : visibleTargets) {
            candidateIds.add(target.uniqueId());
        }
        var visibleIds = visibleTargets.stream().map(Player::uniqueId).collect(java.util.stream.Collectors.toSet());
        for (var targetId : candidateIds) {
            if (targetId.equals(viewerId) || visibleIds.contains(targetId)) {
                tracker.restoreTabListVisibility(viewerId, targetId);
            } else {
                hide(
                        viewerId,
                        targetId,
                        () -> realPlayers.setRealEntryVisible(viewerId, targetId, false),
                        () -> realPlayers.setRealEntryVisible(viewerId, targetId, true));
            }
        }
    }

    private void showOnlyWithPlayerCandidates(Player viewer, Collection<? extends Player> visibleTargets) {
        Set<Player> candidates = new HashSet<>();
        candidates.addAll(viewer.world().players());
        candidates.addAll(visibleTargets);
        var visibleIds = visibleTargets.stream().map(Player::uniqueId).collect(java.util.stream.Collectors.toSet());
        for (var target : candidates) {
            if (target.uniqueId().equals(viewer.uniqueId()) || visibleIds.contains(target.uniqueId())) {
                tracker.restoreTabListVisibility(viewer.uniqueId(), target.uniqueId());
            } else {
                hide(
                        viewer.uniqueId(),
                        target.uniqueId(),
                        () -> delegate.setVisible(viewer, target, false),
                        () -> delegate.setVisible(viewer, target, true));
            }
        }
    }

    private void hide(UUID viewerId, UUID targetId, Runnable hide, Runnable show) {
        visibilityRegistry.hide(tracker, viewerId, targetId, hide);
        tracker.trackTabListVisibility(
                viewerId,
                targetId,
                () -> visibilityRegistry.show(tracker, viewerId, targetId, show));
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
