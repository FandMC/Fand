package io.fand.api.structure;

public enum StructureRotation {
    NONE(0.0F),
    CLOCKWISE_90(90.0F),
    CLOCKWISE_180(180.0F),
    COUNTERCLOCKWISE_90(270.0F);

    private final float degrees;

    StructureRotation(float degrees) {
        this.degrees = degrees;
    }

    public float degrees() {
        return degrees;
    }

    public static StructureRotation fromDegrees(float degrees) {
        var normalized = ((degrees % 360.0F) + 360.0F) % 360.0F;
        if (normalized >= 45.0F && normalized < 135.0F) {
            return CLOCKWISE_90;
        }
        if (normalized >= 135.0F && normalized < 225.0F) {
            return CLOCKWISE_180;
        }
        if (normalized >= 225.0F && normalized < 315.0F) {
            return COUNTERCLOCKWISE_90;
        }
        return NONE;
    }
}
