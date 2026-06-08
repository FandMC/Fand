package io.fand.api.event.player;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a player shears an entity.
 */
public final class PlayerShearEntityEvent implements Event, Cancellable {

    private final Player player;
    private final Entity entity;
    private final PlayerInteractEvent.Hand hand;
    private final ItemStack tool;
    private boolean cancelled;

    public PlayerShearEntityEvent(Player player, Entity entity, PlayerInteractEvent.Hand hand, ItemStack tool) {
        this.player = Objects.requireNonNull(player, "player");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.hand = Objects.requireNonNull(hand, "hand");
        this.tool = Objects.requireNonNull(tool, "tool");
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

    public ItemStack tool() {
        return tool;
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
