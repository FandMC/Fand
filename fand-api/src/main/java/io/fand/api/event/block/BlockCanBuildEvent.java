package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread when vanilla checks whether a block can be placed.
 */
public final class BlockCanBuildEvent implements Event {

    private final Optional<Player> player;
    private final Block block;
    private final BlockType blockType;
    private final ItemStack item;
    private boolean buildable;

    public BlockCanBuildEvent(Optional<Player> player, Block block, BlockType blockType, ItemStack item, boolean buildable) {
        this.player = Objects.requireNonNull(player, "player");
        this.block = Objects.requireNonNull(block, "block");
        this.blockType = Objects.requireNonNull(blockType, "blockType");
        this.item = Objects.requireNonNull(item, "item");
        this.buildable = buildable;
    }

    public Optional<Player> player() {
        return player;
    }

    public Block block() {
        return block;
    }

    public BlockType blockType() {
        return blockType;
    }

    public ItemStack item() {
        return item;
    }

    public boolean buildable() {
        return buildable;
    }

    public void setBuildable(boolean buildable) {
        this.buildable = buildable;
    }
}
