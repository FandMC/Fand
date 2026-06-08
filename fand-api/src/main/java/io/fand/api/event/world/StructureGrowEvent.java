package io.fand.api.event.world;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a tree-like structure grows.
 */
public final class StructureGrowEvent implements Event, Cancellable {

    private final Location location;
    private final Optional<Player> player;
    private final boolean fromBonemeal;
    private final List<Block> blocks;
    private boolean cancelled;

    public StructureGrowEvent(Location location, Optional<Player> player, boolean fromBonemeal, List<? extends Block> blocks) {
        this.location = Objects.requireNonNull(location, "location");
        this.player = Objects.requireNonNull(player, "player");
        this.fromBonemeal = fromBonemeal;
        this.blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
    }

    public Location location() {
        return location;
    }

    public Optional<Player> player() {
        return player;
    }

    public boolean fromBonemeal() {
        return fromBonemeal;
    }

    public List<Block> blocks() {
        return blocks;
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
