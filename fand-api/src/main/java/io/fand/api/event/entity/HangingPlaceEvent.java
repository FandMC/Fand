package io.fand.api.event.entity;

import io.fand.api.block.Block;
import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.event.block.BlockFace;
import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a hanging entity is placed.
 */
public final class HangingPlaceEvent implements Event, Cancellable {

    private final Optional<Player> player;
    private final Entity entity;
    private final Block block;
    private final BlockFace face;
    private final ItemStack item;
    private boolean cancelled;

    public HangingPlaceEvent(Optional<Player> player, Entity entity, Block block, BlockFace face, ItemStack item) {
        this.player = Objects.requireNonNull(player, "player");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.block = Objects.requireNonNull(block, "block");
        this.face = Objects.requireNonNull(face, "face");
        this.item = Objects.requireNonNull(item, "item");
    }

    public Optional<Player> player() {
        return player;
    }

    public Entity entity() {
        return entity;
    }

    public Block block() {
        return block;
    }

    public BlockFace face() {
        return face;
    }

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
