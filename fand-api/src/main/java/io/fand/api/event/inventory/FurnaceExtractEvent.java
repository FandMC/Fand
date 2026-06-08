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
public final class FurnaceExtractEvent implements Event {

    private final Player player;
    private final Inventory inventory;
    private final ItemStack item;
    private final int amount;

    public FurnaceExtractEvent(Player player, Inventory inventory, ItemStack item, int amount) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.item = Objects.requireNonNull(item, "item");
        this.amount = Math.max(0, amount);
    }

    public Player player() {
        return player;
    }

    public Inventory inventory() {
        return inventory;
    }

    public ItemStack item() {
        return item;
    }

    public int amount() {
        return amount;
    }
}
