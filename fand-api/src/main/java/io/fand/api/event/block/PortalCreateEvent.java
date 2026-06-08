package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.world.World;
import java.util.List;
import java.util.Objects;

/**
 * Fired on the server thread before a portal structure is written to the world.
 */
public final class PortalCreateEvent implements Event, Cancellable {

    private final World world;
    private final List<Block> blocks;
    private final Type type;
    private final Cause cause;
    private boolean cancelled;

    public PortalCreateEvent(World world, List<? extends Block> blocks, Type type, Cause cause) {
        this.world = Objects.requireNonNull(world, "world");
        this.blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
        this.type = Objects.requireNonNull(type, "type");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public World world() {
        return world;
    }

    /**
     * Snapshot of blocks that vanilla is about to place for the portal.
     */
    public List<Block> blocks() {
        return blocks;
    }

    public Type type() {
        return type;
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

    public enum Type {
        NETHER,
        END
    }

    public enum Cause {
        FIRE,
        EXIT_PORTAL,
        END_EYE
    }
}
