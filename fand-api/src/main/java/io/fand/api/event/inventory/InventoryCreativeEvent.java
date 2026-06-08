package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a creative-mode slot packet is applied.
 */
public final class InventoryCreativeEvent implements Event, Cancellable {

    private final Player player;
    private final int rawSlot;
    private final boolean drop;
    private ItemStack item;
    private boolean cancelled;

    public InventoryCreativeEvent(Player player, int rawSlot, boolean drop, ItemStack item) {
        this.player = Objects.requireNonNull(player, "player");
        this.rawSlot = rawSlot;
        this.drop = drop;
        this.item = Objects.requireNonNull(item, "item");
    }

    public Player player() {
        return player;
    }

    public int rawSlot() {
        return rawSlot;
    }

    public boolean drop() {
        return drop;
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = Objects.requireNonNull(item, "item");
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
