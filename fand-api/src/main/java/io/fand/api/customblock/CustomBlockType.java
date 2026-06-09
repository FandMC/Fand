package io.fand.api.customblock;

import io.fand.api.block.BlockType;
import io.fand.api.component.DataComponentMap;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Registered custom-block definition backed by a vanilla block type plus Fand
 * persistent components.
 */
public record CustomBlockType(
        Key id,
        BlockType baseType,
        DataComponentMap defaultComponents,
        boolean ticking
) {

    public CustomBlockType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(baseType, "baseType");
        defaultComponents = defaultComponents == null ? DataComponentMap.EMPTY : defaultComponents;
    }

    public CustomBlockType(Key id, BlockType baseType) {
        this(id, baseType, DataComponentMap.EMPTY, false);
    }
}
