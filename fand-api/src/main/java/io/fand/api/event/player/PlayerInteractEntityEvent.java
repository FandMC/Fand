package io.fand.api.event.player;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a player right-clicks an entity.
 */
public final class PlayerInteractEntityEvent implements Event, Cancellable {

    private final Player player;
    private final Entity entity;
    private final PlayerInteractEvent.Hand hand;
    private final ItemStack item;
    private final boolean preciseInteraction;
    private boolean cancelled;

    public PlayerInteractEntityEvent(
            Player player,
            Entity entity,
            PlayerInteractEvent.Hand hand,
            ItemStack item,
            boolean preciseInteraction) {
        this.player = Objects.requireNonNull(player, "player");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.hand = Objects.requireNonNull(hand, "hand");
        this.item = Objects.requireNonNull(item, "item");
        this.preciseInteraction = preciseInteraction;
    }

    public Player player() {
        return player;
    }

    public Entity entity() {
        return entity;
    }

    public PlayerInteractEvent.Hand hand() {
        return hand;
    }

    public ItemStack item() {
        return item;
    }

    /** Whether the client sent an interact-at packet with an exact hit vector. */
    public boolean preciseInteraction() {
        return preciseInteraction;
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
