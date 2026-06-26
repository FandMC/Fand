package io.fand.api.tablist;

import io.fand.api.entity.Player;
import java.util.Collection;

/**
 * Strategy used by proxies or clusters to publish remote player-list rows.
 */
public interface TabListSyncStrategy {

    Collection<RemoteTabListEntry> entries(Player viewer);

    default void apply(TabListService service, Player viewer) {
        java.util.Objects.requireNonNull(service, "service");
        java.util.Objects.requireNonNull(viewer, "viewer");
        for (var remote : entries(viewer)) {
            service.update(viewer, remote.entry());
        }
    }
}
