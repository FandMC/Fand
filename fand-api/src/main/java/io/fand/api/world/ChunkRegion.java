package io.fand.api.world;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Inclusive rectangular chunk-coordinate region.
 */
public record ChunkRegion(int minX, int minZ, int maxX, int maxZ) {

    public ChunkRegion {
        if (minX > maxX) {
            throw new IllegalArgumentException("minX must be <= maxX");
        }
        if (minZ > maxZ) {
            throw new IllegalArgumentException("minZ must be <= maxZ");
        }
    }

    public static ChunkRegion of(int x1, int z1, int x2, int z2) {
        return new ChunkRegion(Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2));
    }

    public static ChunkRegion around(int centerX, int centerZ, int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("radius must be >= 0");
        }
        return new ChunkRegion(centerX - radius, centerZ - radius, centerX + radius, centerZ + radius);
    }

    public static ChunkRegion around(ChunkPos center, int radius) {
        Objects.requireNonNull(center, "center");
        return around(center.x(), center.z(), radius);
    }

    public static ChunkRegion containingBlocks(int blockX1, int blockZ1, int blockX2, int blockZ2) {
        return of(
                Math.floorDiv(blockX1, 16),
                Math.floorDiv(blockZ1, 16),
                Math.floorDiv(blockX2, 16),
                Math.floorDiv(blockZ2, 16));
    }

    public static ChunkRegion containing(Location first, Location second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (!first.world().key().equals(second.world().key())) {
            throw new IllegalArgumentException("locations must be in the same world");
        }
        return containingBlocks(first.blockX(), first.blockZ(), second.blockX(), second.blockZ());
    }

    public long chunkCount() {
        return ((long) maxX - minX + 1L) * ((long) maxZ - minZ + 1L);
    }

    public int width() {
        long width = (long) maxX - minX + 1L;
        return width > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) width;
    }

    public int depth() {
        long depth = (long) maxZ - minZ + 1L;
        return depth > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) depth;
    }

    public ChunkPos center() {
        return new ChunkPos(minX + (maxX - minX) / 2, minZ + (maxZ - minZ) / 2);
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean contains(ChunkPos pos) {
        Objects.requireNonNull(pos, "pos");
        return contains(pos.x(), pos.z());
    }

    public Iterable<ChunkPos> chunks() {
        return new AbstractCollection<>() {
            @Override
            public Iterator<ChunkPos> iterator() {
                return new Iterator<>() {
                    private int x = minX;
                    private int z = minZ;
                    private boolean hasNext = true;

                    @Override
                    public boolean hasNext() {
                        return hasNext;
                    }

                    @Override
                    public ChunkPos next() {
                        if (!hasNext) {
                            throw new NoSuchElementException();
                        }
                        var pos = new ChunkPos(x, z);
                        advance();
                        return pos;
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
                        hasNext = false;
                    }
                };
            }

            @Override
            public int size() {
                long count = chunkCount();
                return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
            }
        };
    }
}
