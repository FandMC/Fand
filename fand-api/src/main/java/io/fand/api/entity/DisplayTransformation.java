package io.fand.api.entity;

import io.fand.api.world.Vector3;
import java.util.Objects;

/**
 * Full client-side transform for display entities.
 */
public record DisplayTransformation(
        Vector3 translation,
        Quaternion leftRotation,
        Vector3 scale,
        Quaternion rightRotation
) {

    public static final DisplayTransformation IDENTITY = new DisplayTransformation(
            Vector3.ZERO,
            Quaternion.IDENTITY,
            new Vector3(1.0, 1.0, 1.0),
            Quaternion.IDENTITY);

    public DisplayTransformation {
        translation = requireFinite(translation, "translation");
        leftRotation = Objects.requireNonNull(leftRotation, "leftRotation");
        scale = requireFinite(scale, "scale");
        rightRotation = Objects.requireNonNull(rightRotation, "rightRotation");
    }

    public DisplayTransformation withTranslation(Vector3 translation) {
        return new DisplayTransformation(translation, leftRotation, scale, rightRotation);
    }

    public DisplayTransformation withLeftRotation(Quaternion leftRotation) {
        return new DisplayTransformation(translation, leftRotation, scale, rightRotation);
    }

    public DisplayTransformation withScale(Vector3 scale) {
        return new DisplayTransformation(translation, leftRotation, scale, rightRotation);
    }

    public DisplayTransformation withRightRotation(Quaternion rightRotation) {
        return new DisplayTransformation(translation, leftRotation, scale, rightRotation);
    }

    private static Vector3 requireFinite(Vector3 value, String name) {
        Objects.requireNonNull(value, name);
        if (!Double.isFinite(value.x()) || !Double.isFinite(value.y()) || !Double.isFinite(value.z())) {
            throw new IllegalArgumentException(name + " coordinates must be finite");
        }
        return value;
    }
}
