package io.fand.api.event.player;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired before a player swaps equipment with an armor stand.
 */
public final class PlayerArmorStandManipulateEvent implements Event, Cancellable {

    private final Player player;
    private final Entity armorStand;
    private final EquipmentSlot slot;
    private final ItemStack playerItem;
    private final ItemStack armorStandItem;
    private boolean cancelled;

    public PlayerArmorStandManipulateEvent(
            Player player,
            Entity armorStand,
            EquipmentSlot slot,
            ItemStack playerItem,
            ItemStack armorStandItem) {
        this.player = Objects.requireNonNull(player, "player");
        this.armorStand = Objects.requireNonNull(armorStand, "armorStand");
        this.slot = Objects.requireNonNull(slot, "slot");
        this.playerItem = Objects.requireNonNull(playerItem, "playerItem");
        this.armorStandItem = Objects.requireNonNull(armorStandItem, "armorStandItem");
    }

    public Player player() {
        return player;
    }

    public Entity armorStand() {
        return armorStand;
    }

    public EquipmentSlot slot() {
        return slot;
    }

    public ItemStack playerItem() {
        return playerItem;
    }

    public ItemStack armorStandItem() {
        return armorStandItem;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public enum EquipmentSlot {
        MAIN_HAND,
        OFF_HAND,
        FEET,
        LEGS,
        CHEST,
        HEAD,
        BODY
    }
}
