package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

public record PlayerRiptideEvent(Player player, ItemStack item) implements Event {

    public PlayerRiptideEvent(Player player, ItemStack item) {
        this.player = Objects.requireNonNull(player, "player");
        this.item = Objects.requireNonNull(item, "item");
    }
}
