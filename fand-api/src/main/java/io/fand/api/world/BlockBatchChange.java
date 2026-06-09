package io.fand.api.world;

import io.fand.api.block.BlockType;
import io.fand.api.component.DataComponentMap;
import java.util.Objects;

/**
 * One block mutation in a batch operation.
 */
public record BlockBatchChange(int x, int y, int z, BlockType type, DataComponentMap components) {

    public BlockBatchChange {
        Objects.requireNonNull(type, "type");
        components = components == null ? DataComponentMap.EMPTY : components;
    }

    public static BlockBatchChange of(int x, int y, int z, BlockType type) {
        return new BlockBatchChange(x, y, z, type, DataComponentMap.EMPTY);
    }

    public static BlockBatchChange of(int x, int y, int z, BlockType type, DataComponentMap components) {
        return new BlockBatchChange(x, y, z, type, components);
    }

    public BlockBatchChange offset(int dx, int dy, int dz) {
        return new BlockBatchChange(x + dx, y + dy, z + dz, type, components);
    }
}
