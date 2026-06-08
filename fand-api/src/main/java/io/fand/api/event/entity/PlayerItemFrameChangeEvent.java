package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a player changes an item frame.
 */
public final class PlayerItemFrameChangeEvent implements Event, Cancellable {

    public enum Action {
        PLACE,
        ROTATE,
        REMOVE
    }

    private final Player player;
    private final Entity itemFrame;
    private final Action action;
    private ItemStack item;
    private int rotation;
    private boolean cancelled;

    public PlayerItemFrameChangeEvent(Player player, Entity itemFrame, Action action, ItemStack item, int rotation) {
        this.player = Objects.requireNonNull(player, "player");
        this.itemFrame = Objects.requireNonNull(itemFrame, "itemFrame");
        this.action = Objects.requireNonNull(action, "action");
        this.item = Objects.requireNonNull(item, "item");
        this.rotation = normalizeRotation(rotation);
    }

    public Player player() {
        return player;
    }

    public Entity itemFrame() {
        return itemFrame;
    }

    public Action action() {
        return action;
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = Objects.requireNonNull(item, "item");
    }

    public int rotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = normalizeRotation(rotation);
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    private static int normalizeRotation(int rotation) {
        return Math.floorMod(rotation, 8);
    }
}
