package io.fand.api.block.custom;

import io.fand.api.block.Block;
import java.util.Objects;

/** Context passed to custom-block lifecycle callbacks. */
public record CustomBlockContext(CustomBlockRegistry registry, CustomBlockType type, Block block) {

    public CustomBlockContext {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(block, "block");
    }
}
