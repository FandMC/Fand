package io.fand.api.entity;

/**
 * Immutable quaternion rotation used by display entity transformations.
 */
public record Quaternion(float x, float y, float z, float w) {

    public static final Quaternion IDENTITY = new Quaternion(0.0F, 0.0F, 0.0F, 1.0F);

    public Quaternion {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(w, "w");
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
