package io.fand.api.world;

import io.fand.api.block.BlockType;
import io.fand.api.component.DataComponentMap;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class FillBlockChanges extends AbstractCollection<BlockBatchChange> {

    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final BlockType type;
    private final DataComponentMap components;
    private final int size;

    FillBlockChanges(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            BlockType type,
            DataComponentMap components,
            int size
    ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.type = type;
        this.components = components;
        this.size = size;
    }

    @Override
    public Iterator<BlockBatchChange> iterator() {
        return new Iterator<>() {
            private int x = minX;
            private int y = minY;
            private int z = minZ;
            private boolean hasNext = size > 0;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public BlockBatchChange next() {
                if (!hasNext) {
                    throw new NoSuchElementException();
                }
                var change = BlockBatchChange.of(x, y, z, type, components);
                advance();
                return change;
            }

            private void advance() {
                if (x < maxX) {
                    x++;
                    return;
                }
                x = minX;
                if (z < maxZ) {
                    z++;
                    return;
                }
                z = minZ;
                if (y < maxY) {
                    y++;
                    return;
                }
                hasNext = false;
            }
        };
    }

    @Override
    public int size() {
        return size;
    }
}
