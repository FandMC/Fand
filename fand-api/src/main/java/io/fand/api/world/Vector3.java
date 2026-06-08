package io.fand.api.world;

/**
 * Immutable three-dimensional vector used for motion and direction.
 */
public record Vector3(double x, double y, double z) {

    public static final Vector3 ZERO = new Vector3(0.0, 0.0, 0.0);

    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 multiply(double factor) {
        return new Vector3(x * factor, y * factor, z * factor);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }
}
