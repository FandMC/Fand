package io.fand.api.packet;

/**
 * An immutable double-precision 3D vector, used by packet views for vanilla
 * {@code Vec3} fields (positions, velocities). NMS-free.
 */
public record Vec3d(double x, double y, double z) {

    public static final Vec3d ZERO = new Vec3d(0.0, 0.0, 0.0);
}
