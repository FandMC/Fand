package io.fand.api.world;

import java.util.Objects;

/**
 * Immutable position in a {@link World}. Yaw is in degrees (0 = south, increases
 * clockwise looking down) and pitch is in degrees (-90 looking up, 90 looking
 * down) matching the Minecraft conventions.
 */
public record Location(World world, double x, double y, double z, float yaw, float pitch) {

    public Location {
        Objects.requireNonNull(world, "world");
    }

    public Location withCoordinates(double x, double y, double z) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Location withRotation(float yaw, float pitch) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Location offset(double dx, double dy, double dz) {
        return new Location(world, x + dx, y + dy, z + dz, yaw, pitch);
    }

    public int blockX() {
        return (int) Math.floor(x);
    }

    public int blockY() {
        return (int) Math.floor(y);
    }

    public int blockZ() {
        return (int) Math.floor(z);
    }

    public ChunkPos chunkPos() {
        return ChunkPos.containing(this);
    }

    public Vector3 horizontalDirection() {
        double radians = Math.toRadians(yaw);
        return new Vector3(-Math.sin(radians), 0.0D, Math.cos(radians));
    }
}
