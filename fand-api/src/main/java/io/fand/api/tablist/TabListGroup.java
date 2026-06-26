package io.fand.api.tablist;

import io.fand.api.entity.Player;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A reusable per-viewer player-list grouping rule.
 */
public record TabListGroup(
        Predicate<Player> filter,
        Comparator<Player> order,
        int orderBase
) {

    public TabListGroup {
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(order, "order");
    }

    public static TabListGroup of(Predicate<Player> filter) {
        return new TabListGroup(filter, Comparator.comparing(Player::name), 0);
    }

    public TabListGroup withOrder(Comparator<Player> order) {
        return new TabListGroup(filter, order, orderBase);
    }

    public TabListGroup withOrderBase(int orderBase) {
        return new TabListGroup(filter, order, orderBase);
    }

    public List<Player> select(Collection<? extends Player> players) {
        Objects.requireNonNull(players, "players");
        return players.stream()
                .filter(filter)
                .map(player -> (Player) player)
                .sorted(order)
                .toList();
    }

    public List<TabListEntry> entries(Collection<? extends Player> players) {
        var selected = select(players);
        var entries = new java.util.ArrayList<TabListEntry>(selected.size());
        for (int i = 0; i < selected.size(); i++) {
            var player = selected.get(i);
            entries.add(TabListEntry.builder(player.profile())
                    .latency(player.ping())
                    .gameMode(player.gameMode())
                    .displayName(player.tabListDisplayName().orElse(null))
                    .order(orderBase + i)
                    .build());
        }
        return List.copyOf(entries);
    }
}
