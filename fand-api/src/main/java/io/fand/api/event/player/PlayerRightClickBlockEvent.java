package io.fand.api.event.player;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.event.block.BlockFace;
import io.fand.api.item.ItemStack;
import io.fand.api.world.Vector3;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before a player right-clicks a block.
 */
public class PlayerRightClickBlockEvent extends PlayerInteractEvent {

    private final Block clickedBlock;
    private final @Nullable BlockFace clickedFace;
    private final @Nullable Vector3 hitPosition;

    public PlayerRightClickBlockEvent(Player player, Hand hand, Block block, ItemStack item) {
        this(player, hand, block, item, null);
    }

    public PlayerRightClickBlockEvent(
            Player player,
            Hand hand,
            Block block,
            ItemStack item,
            @Nullable BlockFace clickedFace
    ) {
        this(player, hand, block, item, clickedFace, null);
    }

    public PlayerRightClickBlockEvent(
            Player player,
            Hand hand,
            Block block,
            ItemStack item,
            @Nullable BlockFace clickedFace,
            @Nullable Vector3 hitPosition
    ) {
        super(player, Action.RIGHT_CLICK_BLOCK, hand, block, item);
        this.clickedBlock = Objects.requireNonNull(block, "block");
        this.clickedFace = clickedFace;
        this.hitPosition = hitPosition;
    }

    public Block clickedBlock() {
        return clickedBlock;
    }

    /** Face targeted by the client; empty only for events constructed through the legacy constructor. */
    public Optional<BlockFace> clickedFace() {
        return Optional.ofNullable(clickedFace);
    }

    /** Exact world-space position targeted by the client. */
    public Optional<Vector3> hitPosition() {
        return Optional.ofNullable(hitPosition);
    }

    /** Exact hit position relative to the clicked block's lower corner. */
    public Optional<Vector3> relativeHitPosition() {
        return hitPosition().map(hit -> new Vector3(
                hit.x() - clickedBlock.x(),
                hit.y() - clickedBlock.y(),
                hit.z() - clickedBlock.z()));
    }
}
