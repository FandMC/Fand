package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.inventory.InventoryType;
import java.util.Objects;

/**
 * Fired on the server thread after a player closes a container. Not
 * cancellable — by the time this fires, the client has already torn down
 * its UI.
 */
public record InventoryCloseEvent(Player player, InventoryType type) implements Event {

    public InventoryCloseEvent(Player player, InventoryType type) {
        this.player = Objects.requireNonNull(player, "player");
        this.type = Objects.requireNonNull(type, "type");
    }
}
