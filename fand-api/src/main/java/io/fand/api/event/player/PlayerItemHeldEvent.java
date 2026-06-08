package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a player changes the selected hotbar slot.
 */
public final class PlayerItemHeldEvent implements Event, Cancellable {

    private final Player player;
    private final int previousSlot;
    private final int newSlot;
    private final ItemStack previousItem;
    private final ItemStack newItem;
    private boolean cancelled;

    public PlayerItemHeldEvent(
            Player player,
            int previousSlot,
            int newSlot,
            ItemStack previousItem,
            ItemStack newItem) {
        this.player = Objects.requireNonNull(player, "player");
        this.previousSlot = previousSlot;
        this.newSlot = newSlot;
        this.previousItem = Objects.requireNonNull(previousItem, "previousItem");
        this.newItem = Objects.requireNonNull(newItem, "newItem");
    }

    public Player player() {
        return player;
    }

    public int previousSlot() {
        return previousSlot;
    }

    public int newSlot() {
        return newSlot;
    }

    public ItemStack previousItem() {
        return previousItem;
    }

    public ItemStack newItem() {
        return newItem;
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
