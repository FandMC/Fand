package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread immediately after a placement context resolves
 * to a concrete block state but before the block is committed to the world.
 * Cancelling the event aborts the placement; the placing item is not consumed
 * and the client view is resynced.
 */
public final class BlockPlaceEvent implements Event, Cancellable {

    private final Player player;
    private final Block block;
    private final BlockType placedType;
    private final BlockType replacedType;
    private boolean cancelled;

    public BlockPlaceEvent(Player player, Block block, BlockType placedType, BlockType replacedType) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        this.placedType = Objects.requireNonNull(placedType, "placedType");
        this.replacedType = Objects.requireNonNull(replacedType, "replacedType");
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

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
