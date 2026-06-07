package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a player-spawned item entity is added to
 * the world. Cancelling the event suppresses the drop.
 *
 * <p>The item stack is mutable so listeners can replace what will be spawned.
 * Explicit hand drops (for example pressing Q) are restored to the player if
 * cancelled by the runtime.
 */
public final class PlayerDropItemEvent implements Event, Cancellable {

    private final Player player;
    private final boolean randomMotion;
    private final boolean thrownFromHand;
    private ItemStack item;
    private boolean cancelled;

    public PlayerDropItemEvent(Player player, ItemStack item, boolean randomMotion, boolean thrownFromHand) {
        this.player = Objects.requireNonNull(player, "player");
        this.item = Objects.requireNonNull(item, "item");
        this.randomMotion = randomMotion;
        this.thrownFromHand = thrownFromHand;
    }

    public Player player() {
        return player;
    }

    /** Item stack that will be spawned if the event is not cancelled. */
    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = Objects.requireNonNull(item, "item");
    }

    /** Whether vanilla will apply random drop motion. */
    public boolean randomMotion() {
        return randomMotion;
    }

    /** Whether vanilla considers this drop thrown by the player's hand. */
    public boolean thrownFromHand() {
        return thrownFromHand;
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
