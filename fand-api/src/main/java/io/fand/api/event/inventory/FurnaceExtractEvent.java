package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread when a player takes items from a furnace result
 * slot.
 */
public record FurnaceExtractEvent(Player player, Inventory inventory, ItemStack item, int amount) implements Event {

    public FurnaceExtractEvent(Player player, Inventory inventory, ItemStack item, int amount) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.item = Objects.requireNonNull(item, "item");
        this.amount = Math.max(0, amount);
    }
}
