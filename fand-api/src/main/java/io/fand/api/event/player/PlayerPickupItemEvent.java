package io.fand.api.event.player;

import io.fand.api.entity.ItemEntity;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a player attempts to pick up an item
 * entity. Cancelling the event leaves the item entity in the world.
 *
 * <p>The item stack is mutable so listeners can replace the stack before
 * vanilla tries to merge it into the player's inventory.
 */
public final class PlayerPickupItemEvent implements Event, Cancellable {

    private final Player player;
    private final ItemEntity itemEntity;
    private ItemStack item;
    private boolean cancelled;

    public PlayerPickupItemEvent(Player player, ItemEntity itemEntity, ItemStack item) {
        this.player = Objects.requireNonNull(player, "player");
        this.itemEntity = Objects.requireNonNull(itemEntity, "itemEntity");
        this.item = Objects.requireNonNull(item, "item");
    }

    public Player player() {
        return player;
    }

    public ItemEntity itemEntity() {
        return itemEntity;
    }

    /** Item stack that vanilla will try to merge into the inventory. */
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
