package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.InventoryType;
import java.util.Objects;

/**
 * Fired on the server thread when a player is about to open a container.
 * Cancelling the event prevents the menu from opening. The player's own
 * inventory ({@link InventoryType#PLAYER}) is not surfaced here.
 */
public final class InventoryOpenEvent implements Event, Cancellable {

    private final Player player;
    private final InventoryType type;
    private boolean cancelled;

    public InventoryOpenEvent(Player player, InventoryType type) {
        this.player = Objects.requireNonNull(player, "player");
        this.type = Objects.requireNonNull(type, "type");
    }

    public Player player() {
        return player;
    }

    public InventoryType type() {
        return type;
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
