package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a merchant trade result is shown.
 */
public final class PrepareTradeEvent implements Event {

    private final Player player;
    private final Inventory inventory;
    private final ItemStack firstCost;
    private final ItemStack secondCost;
    private final int villagerExperience;
    private ItemStack result;

    public PrepareTradeEvent(
            Player player,
            Inventory inventory,
            ItemStack firstCost,
            ItemStack secondCost,
            ItemStack result,
            int villagerExperience) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.firstCost = Objects.requireNonNull(firstCost, "firstCost");
        this.secondCost = Objects.requireNonNull(secondCost, "secondCost");
        this.result = Objects.requireNonNull(result, "result");
        this.villagerExperience = Math.max(0, villagerExperience);
    }

    public Player player() {
        return player;
    }

    public Inventory inventory() {
        return inventory;
    }

    public ItemStack firstCost() {
        return firstCost;
    }

    public ItemStack secondCost() {
        return secondCost;
    }

    public ItemStack result() {
        return result;
    }

    public void setResult(ItemStack result) {
        this.result = Objects.requireNonNull(result, "result");
    }

    public int villagerExperience() {
        return villagerExperience;
    }
}
