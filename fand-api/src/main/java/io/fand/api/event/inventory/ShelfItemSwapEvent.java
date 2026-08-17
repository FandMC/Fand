package io.fand.api.event.inventory;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.List;
import java.util.Objects;

/**
 * Fired before a player exchanges one or more inventory slots with a Shelf.
 * Powered multi-Shelf hotbar exchanges are represented by one atomic event.
 */
public final class ShelfItemSwapEvent implements Event, Cancellable {

    private final Player player;
    private final Block shelf;
    private final boolean powered;
    private final List<Exchange> exchanges;
    private boolean cancelled;

    public ShelfItemSwapEvent(Player player, Block shelf, boolean powered, List<Exchange> exchanges) {
        this.player = Objects.requireNonNull(player, "player");
        this.shelf = Objects.requireNonNull(shelf, "shelf");
        this.powered = powered;
        this.exchanges = List.copyOf(Objects.requireNonNull(exchanges, "exchanges"));
    }

    public Player player() {
        return player;
    }

    public Block shelf() {
        return shelf;
    }

    public boolean powered() {
        return powered;
    }

    public List<Exchange> exchanges() {
        return exchanges;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /** One planned exchange, captured before either slot is mutated. */
    public record Exchange(
            Block shelf,
            int shelfSlot,
            int playerSlot,
            ItemStack shelfItem,
            ItemStack playerItem
    ) {
        public Exchange {
            Objects.requireNonNull(shelf, "shelf");
            Objects.requireNonNull(shelfItem, "shelfItem");
            Objects.requireNonNull(playerItem, "playerItem");
            if (shelfSlot < 0 || playerSlot < 0) {
                throw new IllegalArgumentException("Slot indices must be non-negative");
            }
        }
    }
}
