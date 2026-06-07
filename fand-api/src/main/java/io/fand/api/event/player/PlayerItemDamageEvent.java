package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before durability damage is applied to a player's item.
 */
public final class PlayerItemDamageEvent implements Event, Cancellable {

    private final Player player;
    private final ItemStack item;
    private int damage;
    private boolean cancelled;

    public PlayerItemDamageEvent(Player player, ItemStack item, int damage) {
        this.player = Objects.requireNonNull(player, "player");
        this.item = Objects.requireNonNull(item, "item");
        this.damage = damage;
    }

    public Player player() {
        return player;
    }

    public ItemStack item() {
        return item;
    }

    public int damage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = Math.max(0, damage);
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
