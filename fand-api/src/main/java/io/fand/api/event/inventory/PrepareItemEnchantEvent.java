package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fired on the server thread before enchanting-table offers are shown.
 */
public final class PrepareItemEnchantEvent implements Event {

    private final Player player;
    private final Inventory inventory;
    private final ItemStack item;
    private final int bookshelfPower;
    private final List<EnchantmentOffer> offers;

    public PrepareItemEnchantEvent(
            Player player,
            Inventory inventory,
            ItemStack item,
            int bookshelfPower,
            List<EnchantmentOffer> offers) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.item = Objects.requireNonNull(item, "item");
        this.bookshelfPower = Math.max(0, bookshelfPower);
        this.offers = new ArrayList<>(Objects.requireNonNull(offers, "offers"));
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

    public int bookshelfPower() {
        return bookshelfPower;
    }

    public List<EnchantmentOffer> offers() {
        return offers;
    }
}
