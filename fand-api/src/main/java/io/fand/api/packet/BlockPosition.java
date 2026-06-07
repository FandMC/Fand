package io.fand.api.packet;

/**
 * An immutable integer block position, used by packet views for vanilla
 * {@code BlockPos} fields. NMS-free.
 */
public record BlockPosition(int x, int y, int z) {
}
