package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fired on the server thread before an enchanting-table result is committed.
 */
public final class EnchantItemEvent implements Event, Cancellable {

    private final Player player;
    private final Inventory inventory;
    private final ItemStack inputItem;
    private ItemStack resultItem;
    private final int button;
    private final int levelCost;
    private final int xpCost;
    private final List<EnchantmentOffer> enchantments;
    private boolean cancelled;

    public EnchantItemEvent(
            Player player,
            Inventory inventory,
            ItemStack inputItem,
            ItemStack resultItem,
            int button,
            int levelCost,
            int xpCost,
            List<EnchantmentOffer> enchantments) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.inputItem = Objects.requireNonNull(inputItem, "inputItem");
        this.resultItem = Objects.requireNonNull(resultItem, "resultItem");
        this.button = button;
        this.levelCost = Math.max(0, levelCost);
        this.xpCost = Math.max(0, xpCost);
        this.enchantments = new ArrayList<>(Objects.requireNonNull(enchantments, "enchantments"));
    }

    public Player player() {
        return player;
    }

    public Inventory inventory() {
        return inventory;
    }

    public ItemStack inputItem() {
        return inputItem;
    }

    public ItemStack resultItem() {
        return resultItem;
    }

    public void setResultItem(ItemStack resultItem) {
        this.resultItem = Objects.requireNonNull(resultItem, "resultItem");
    }

    public int button() {
        return button;
    }

    public int levelCost() {
        return levelCost;
    }

    public int xpCost() {
        return xpCost;
    }

    public List<EnchantmentOffer> enchantments() {
        return enchantments;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
