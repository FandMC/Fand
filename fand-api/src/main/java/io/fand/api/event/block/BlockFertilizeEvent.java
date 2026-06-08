package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before bonemeal-style fertilization is applied.
 */
public final class BlockFertilizeEvent implements Event, Cancellable {

    private final Optional<Player> player;
    private final Block block;
    private final ItemStack item;
    private final Cause cause;
    private boolean cancelled;

    public BlockFertilizeEvent(Optional<Player> player, Block block, ItemStack item, Cause cause) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        this.item = Objects.requireNonNull(item, "item");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public Optional<Player> player() {
        return player;
    }

    public Block block() {
        return block;
    }

    public ItemStack item() {
        return item;
    }

    public Cause cause() {
        return cause;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public enum Cause {
        BONE_MEAL,
        WATER_PLANT,
        UNKNOWN
    }
}
