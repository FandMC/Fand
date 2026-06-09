package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.entity.Entity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread before a cauldron changes fill state.
 */
public final class CauldronLevelChangeEvent implements Event, Cancellable {

    private final Block block;
    private final BlockType oldType;
    private final BlockType newType;
    private final int oldLevel;
    private final int newLevel;
    private final @Nullable Entity entity;
    private final Cause cause;
    private boolean cancelled;

    public CauldronLevelChangeEvent(
            Block block,
            BlockType oldType,
            BlockType newType,
            int oldLevel,
            int newLevel,
            @Nullable Entity entity,
            Cause cause
    ) {
        this.block = Objects.requireNonNull(block, "block");
        this.oldType = Objects.requireNonNull(oldType, "oldType");
        this.newType = Objects.requireNonNull(newType, "newType");
        this.oldLevel = clampLevel(oldLevel);
        this.newLevel = clampLevel(newLevel);
        this.entity = entity;
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public Block block() {
        return block;
    }

    public BlockType oldType() {
        return oldType;
    }

    public BlockType newType() {
        return newType;
    }

    public int oldLevel() {
        return oldLevel;
    }

    public int newLevel() {
        return newLevel;
    }

    public Optional<Entity> entity() {
        return Optional.ofNullable(entity);
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

    private static int clampLevel(int level) {
        return Math.max(0, Math.min(3, level));
    }

    public enum Cause {
        BUCKET_FILL,
        BUCKET_EMPTY,
        BOTTLE_FILL,
        BOTTLE_EMPTY,
        CLEAN_ITEM,
        ENTITY_EXTINGUISH,
        PRECIPITATION,
        DRIPSTONE,
        UNKNOWN
    }
}
