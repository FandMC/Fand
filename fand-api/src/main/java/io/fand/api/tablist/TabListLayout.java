package io.fand.api.tablist;

import io.fand.api.entity.Player;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A computed set of tab-list rows for one viewer.
 */
public record TabListLayout(List<TabListEntry> entries) {

    public TabListLayout {
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public static TabListLayout of(Collection<? extends TabListEntry> entries) {
        return new TabListLayout(List.copyOf(entries));
    }

    public static TabListLayout from(TabListGroup group, Collection<? extends Player> players) {
        Objects.requireNonNull(group, "group");
        return new TabListLayout(group.entries(players));
    }

    public void apply(TabListService service, Player viewer) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(viewer, "viewer");
        for (var entry : entries) {
            service.update(viewer, entry);
        }
    }
}
