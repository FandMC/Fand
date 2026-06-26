package io.fand.api.tablist;

import io.fand.api.entity.Player;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Per-viewer tab-list entry service.
 */
public interface TabListService {

    default boolean visible(Player viewer, Player target) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(target, "target");
        return target.visibleInPlayerList(viewer);
    }

    default void setVisible(Player viewer, Player target, boolean visible) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(target, "target");
        target.setVisibleInPlayerList(viewer, visible);
    }

    default void showOnly(Player viewer, Collection<? extends Player> visibleTargets) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(visibleTargets, "visibleTargets");
        Set<UUID> visibleIds = new HashSet<>();
        for (var target : visibleTargets) {
            Objects.requireNonNull(target, "visibleTargets cannot contain null");
            visibleIds.add(target.uniqueId());
        }
        for (var target : viewer.world().players()) {
            setVisible(viewer, target, target.uniqueId().equals(viewer.uniqueId()) || visibleIds.contains(target.uniqueId()));
        }
    }

    Collection<? extends TabListRegistration> entries(Player viewer);

    Optional<? extends TabListRegistration> entry(Player viewer, UUID entryId);

    TabListRegistration add(Player viewer, TabListEntry entry);

    default void update(Player viewer, TabListEntry entry) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(entry, "entry");
        entry(viewer, entry.profile().uniqueId()).ifPresentOrElse(
                registration -> registration.update(entry),
                () -> add(viewer, entry));
    }

    default void apply(Player viewer, TabListLayout layout) {
        Objects.requireNonNull(layout, "layout");
        layout.apply(this, viewer);
    }

    default void sync(Player viewer, TabListSyncStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy");
        strategy.apply(this, viewer);
    }

    boolean remove(Player viewer, UUID entryId);

    default boolean remove(Player viewer, TabListEntry entry) {
        return remove(viewer, entry.profile().uniqueId());
    }

    void removeAll(Player viewer);

    static TabListService empty() {
        return new TabListService() {
            @Override
            public Collection<? extends TabListRegistration> entries(Player viewer) {
                Objects.requireNonNull(viewer, "viewer");
                return List.of();
            }

            @Override
            public Optional<? extends TabListRegistration> entry(Player viewer, UUID entryId) {
                Objects.requireNonNull(viewer, "viewer");
                Objects.requireNonNull(entryId, "entryId");
                return Optional.empty();
            }

            @Override
            public TabListRegistration add(Player viewer, TabListEntry entry) {
                throw new UnsupportedOperationException("Per-viewer tab-list entries are not supported");
            }

            @Override
            public boolean remove(Player viewer, UUID entryId) {
                Objects.requireNonNull(viewer, "viewer");
                Objects.requireNonNull(entryId, "entryId");
                return false;
            }

            @Override
            public void removeAll(Player viewer) {
                Objects.requireNonNull(viewer, "viewer");
            }
        };
    }
}
