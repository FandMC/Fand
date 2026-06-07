package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a player swaps main-hand and off-hand items.
 */
public final class PlayerSwapHandItemsEvent implements Event, Cancellable {

    private final Player player;
    private ItemStack mainHandItem;
    private ItemStack offHandItem;
    private boolean cancelled;

    public PlayerSwapHandItemsEvent(Player player, ItemStack mainHandItem, ItemStack offHandItem) {
        this.player = Objects.requireNonNull(player, "player");
        this.mainHandItem = Objects.requireNonNull(mainHandItem, "mainHandItem");
        this.offHandItem = Objects.requireNonNull(offHandItem, "offHandItem");
    }

    public Player player() {
        return player;
    }

    public ItemStack mainHandItem() {
        return mainHandItem;
    }

    public void setMainHandItem(ItemStack mainHandItem) {
        this.mainHandItem = Objects.requireNonNull(mainHandItem, "mainHandItem");
    }

    public ItemStack offHandItem() {
        return offHandItem;
    }

    public void setOffHandItem(ItemStack offHandItem) {
        this.offHandItem = Objects.requireNonNull(offHandItem, "offHandItem");
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
