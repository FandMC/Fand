package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.event.player.PlayerInteractEvent;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread immediately after a placement context resolves
 * to a concrete block state but before the block is committed to the world.
 * Cancelling the event aborts the placement; the placing item is not consumed
 * and the client view is resynced.
 */
public class BlockPlaceEvent implements Event, Cancellable {

    private final Player player;
    private final Block block;
    private final BlockType placedType;
    private final BlockType replacedType;
    private final PlayerInteractEvent.Hand hand;
    private final ItemStack item;
    private boolean cancelled;

    public BlockPlaceEvent(Player player, Block block, BlockType placedType, BlockType replacedType) {
        this(player, block, placedType, replacedType, PlayerInteractEvent.Hand.MAIN_HAND, ItemStack.EMPTY);
    }

    public BlockPlaceEvent(
            Player player,
            Block block,
            BlockType placedType,
            BlockType replacedType,
            PlayerInteractEvent.Hand hand,
            ItemStack item
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        this.placedType = Objects.requireNonNull(placedType, "placedType");
        this.replacedType = Objects.requireNonNull(replacedType, "replacedType");
        this.hand = Objects.requireNonNull(hand, "hand");
        this.item = Objects.requireNonNull(item, "item");
    }

    public Player player() {
        return player;
    }

    public Block block() {
        return block;
    }

    /** Block type that will be set if the event is not cancelled. */
    public BlockType placedType() {
        return placedType;
    }

    /** Block type the placement is overwriting (often air or a replaceable block). */
    public BlockType replacedType() {
        return replacedType;
    }

    /** Hand that performed the placement. */
    public PlayerInteractEvent.Hand hand() {
        return hand;
    }

    /** Item stack used for the placement. */
    public ItemStack item() {
        return item;
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
