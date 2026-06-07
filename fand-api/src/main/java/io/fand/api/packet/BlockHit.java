package io.fand.api.packet;

/**
 * An immutable ray-trace hit against a block, marshalled from a vanilla
 * {@code BlockHitResult}. Carries the clicked block, the face that was hit, the
 * precise hit point, and whether the ray started inside the block. NMS-free.
 */
public record BlockHit(BlockPosition block, String direction, Vec3d location, boolean insideBlock) {

    /**
     * The position a block would be placed at: the clicked block offset by the
     * hit face. Returns {@link #block} unchanged for an unrecognised direction.
     */
    public BlockPosition placed() {
        return switch (direction) {
            case "UP" -> new BlockPosition(block.x(), block.y() + 1, block.z());
            case "DOWN" -> new BlockPosition(block.x(), block.y() - 1, block.z());
            case "NORTH" -> new BlockPosition(block.x(), block.y(), block.z() - 1);
            case "SOUTH" -> new BlockPosition(block.x(), block.y(), block.z() + 1);
            case "WEST" -> new BlockPosition(block.x() - 1, block.y(), block.z());
            case "EAST" -> new BlockPosition(block.x() + 1, block.y(), block.z());
            default -> block;
        };
    }
}
