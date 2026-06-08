package io.fand.api.event.entity;

import io.fand.api.block.Block;
import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fired on the server thread after an entity explosion has calculated affected
 * blocks, but before those blocks are broken.
 */
public final class EntityExplodeEvent implements Event, Cancellable {

    private final Entity entity;
    private final Location location;
    private final List<Block> affectedBlocks;
    private boolean cancelled;

    public EntityExplodeEvent(Entity entity, Location location, List<? extends Block> affectedBlocks) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.location = Objects.requireNonNull(location, "location");
        this.affectedBlocks = new ArrayList<>(Objects.requireNonNull(affectedBlocks, "affectedBlocks"));
    }

    public Entity entity() {
        return entity;
    }

    public Location location() {
        return location;
    }

    /**
     * Live mutable list of blocks vanilla is about to break. Remove entries to
     * protect blocks from this explosion.
     */
    public List<Block> affectedBlocks() {
        return affectedBlocks;
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
