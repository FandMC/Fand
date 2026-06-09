package io.fand.api.entity;

import io.fand.api.block.BlockType;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Minecart-specific controls shared by minecart variants. */
public interface Minecart extends Vehicle {

    boolean onRails();

    boolean flipped();

    void setFlipped(boolean flipped);

    Optional<? extends BlockType> customDisplayBlock();

    void setCustomDisplayBlock(@Nullable BlockType type);

    int displayOffset();

    void setDisplayOffset(int offset);

    boolean rideable();

    boolean furnace();
}
