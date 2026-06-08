package io.fand.api.event.player;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Fired on the server thread before a player empties a bucket into a block.
 */
public final class PlayerBucketEmptyEvent implements Event, Cancellable {

    private final Player player;
    private final Block block;
    private final Key fluid;
    private final ItemStack bucket;
    private ItemStack resultItem;
    private boolean cancelled;

    public PlayerBucketEmptyEvent(Player player, Block block, Key fluid, ItemStack bucket, ItemStack resultItem) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        this.fluid = Objects.requireNonNull(fluid, "fluid");
        this.bucket = Objects.requireNonNull(bucket, "bucket");
        this.resultItem = Objects.requireNonNull(resultItem, "resultItem");
    }

    public Player player() {
        return player;
    }

    /** Block receiving the bucket contents. */
    public Block block() {
        return block;
    }

    /** Vanilla fluid registry key, e.g. {@code minecraft:water}. */
    public Key fluid() {
        return fluid;
    }

    public ItemStack bucket() {
        return bucket;
    }

    public ItemStack resultItem() {
        return resultItem;
    }

    public void setResultItem(ItemStack resultItem) {
        this.resultItem = Objects.requireNonNull(resultItem, "resultItem");
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
