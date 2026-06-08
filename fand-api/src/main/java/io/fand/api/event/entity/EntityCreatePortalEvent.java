package io.fand.api.event.entity;

import io.fand.api.block.Block;
import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.List;
import java.util.Objects;

/**
 * Fired on the server thread before an entity-created portal is written.
 */
public final class EntityCreatePortalEvent implements Event, Cancellable {

    private final Entity entity;
    private final List<Block> blocks;
    private boolean cancelled;

    public EntityCreatePortalEvent(Entity entity, List<? extends Block> blocks) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
    }

    public Entity entity() {
        return entity;
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
