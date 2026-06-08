package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.List;
import java.util.Objects;

/**
 * Fired on the server thread before a sponge absorbs water blocks.
 */
public final class SpongeAbsorbEvent implements Event, Cancellable {

    private final Block sponge;
    private final List<Block> absorbedBlocks;
    private boolean cancelled;

    public SpongeAbsorbEvent(Block sponge, List<? extends Block> absorbedBlocks) {
        this.sponge = Objects.requireNonNull(sponge, "sponge");
        this.absorbedBlocks = List.copyOf(Objects.requireNonNull(absorbedBlocks, "absorbedBlocks"));
    }

    public Block sponge() {
        return sponge;
    }

    public List<Block> absorbedBlocks() {
        return absorbedBlocks;
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
