package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before an anvil result is shown.
 */
public final class PrepareAnvilEvent implements Event {

    private final Player player;
    private final Inventory inventory;
    private final ItemStack firstItem;
    private final ItemStack secondItem;
    private ItemStack result;
    private int cost;
    private final Optional<String> renameText;

    public PrepareAnvilEvent(
            Player player,
            Inventory inventory,
            ItemStack firstItem,
            ItemStack secondItem,
            ItemStack result,
            int cost,
            Optional<String> renameText) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.firstItem = Objects.requireNonNull(firstItem, "firstItem");
        this.secondItem = Objects.requireNonNull(secondItem, "secondItem");
        this.result = Objects.requireNonNull(result, "result");
        this.cost = Math.max(0, cost);
        this.renameText = Objects.requireNonNull(renameText, "renameText");
    }

    public Player player() {
        return player;
    }

    public Inventory inventory() {
        return inventory;
    }

    public ItemStack firstItem() {
        return firstItem;
    }

    public ItemStack secondItem() {
        return secondItem;
    }

    public ItemStack result() {
        return result;
    }

    public void setResult(ItemStack result) {
        this.result = Objects.requireNonNull(result, "result");
    }

    public int cost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = Math.max(0, cost);
    }

    public Optional<String> renameText() {
        return renameText;
    }
}
