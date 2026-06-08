package io.fand.api.event.player;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a player fills an empty bucket from a block.
 */
public final class PlayerBucketFillEvent implements Event, Cancellable {

    private final Player player;
    private final Block block;
    private final ItemStack bucket;
    private ItemStack resultItem;
    private boolean cancelled;

    public PlayerBucketFillEvent(Player player, Block block, ItemStack bucket, ItemStack resultItem) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        this.bucket = Objects.requireNonNull(bucket, "bucket");
        this.resultItem = Objects.requireNonNull(resultItem, "resultItem");
    }

    public Player player() {
        return player;
    }

    /** Block the bucket is taking fluid or powder snow from. */
    public Block block() {
        return block;
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
