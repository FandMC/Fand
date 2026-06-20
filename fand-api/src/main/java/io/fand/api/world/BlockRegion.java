package io.fand.api.world;

/**
 * Inclusive cuboid block region.
 */
public record BlockRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public BlockRegion {
        if (minX > maxX) {
            throw new IllegalArgumentException("minX must be <= maxX");
        }
        if (minY > maxY) {
            throw new IllegalArgumentException("minY must be <= maxY");
        }
        if (minZ > maxZ) {
            throw new IllegalArgumentException("minZ must be <= maxZ");
        }
    }

    public static BlockRegion between(Location first, Location second) {
        java.util.Objects.requireNonNull(first, "first");
        java.util.Objects.requireNonNull(second, "second");
        if (!first.world().key().equals(second.world().key())) {
            throw new IllegalArgumentException("locations must be in the same world");
        }
        return new BlockRegion(
                Math.min(first.blockX(), second.blockX()),
                Math.min(first.blockY(), second.blockY()),
                Math.min(first.blockZ(), second.blockZ()),
                Math.max(first.blockX(), second.blockX()),
                Math.max(first.blockY(), second.blockY()),
                Math.max(first.blockZ(), second.blockZ()));
    }

    public static BlockRegion cube(Location center, int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("radius must not be negative");
        }
        int x = center.blockX();
        int y = center.blockY();
        int z = center.blockZ();
        return new BlockRegion(
                Math.subtractExact(x, radius),
                Math.subtractExact(y, radius),
                Math.subtractExact(z, radius),
                Math.addExact(x, radius),
                Math.addExact(y, radius),
                Math.addExact(z, radius));
    }

    public long cappedVolume() {
        return BlockBatchVolumes.cappedVolume(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
