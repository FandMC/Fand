package io.fand.api.entity;

import io.fand.api.block.BlockType;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Enderman-specific state. */
public interface Enderman extends Mob {

    /** Block type currently carried by this enderman, if any. */
    Optional<? extends BlockType> carriedBlock();

    /** Sets or clears the carried block, using the block type's default state. */
    void setCarriedBlock(@Nullable BlockType type);
}
