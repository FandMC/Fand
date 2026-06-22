package io.fand.api.entity;

import java.util.Locale;

/**
 * Base display entity.
 */
public interface Display extends Entity {

    DisplayTransformation transformation();

    void setTransformation(DisplayTransformation transformation);

    default void setTranslation(io.fand.api.world.Vector3 translation) {
        setTransformation(transformation().withTranslation(translation));
    }

    default void setScale(io.fand.api.world.Vector3 scale) {
        setTransformation(transformation().withScale(scale));
    }

    default void setLeftRotation(Quaternion rotation) {
        setTransformation(transformation().withLeftRotation(rotation));
    }

    default void setRightRotation(Quaternion rotation) {
        setTransformation(transformation().withRightRotation(rotation));
    }

    int transformationInterpolationDuration();

    void setTransformationInterpolationDuration(int ticks);

    int transformationInterpolationDelay();

    void setTransformationInterpolationDelay(int ticks);

    int positionRotationInterpolationDuration();

    void setPositionRotationInterpolationDuration(int ticks);

    Billboard billboard();

    void setBillboard(Billboard billboard);

    int packedBrightnessOverride();

    void setPackedBrightnessOverride(int brightness);

    void clearBrightnessOverride();

    float viewRange();

    void setViewRange(float range);

    float shadowRadius();

    void setShadowRadius(float radius);

    float shadowStrength();

    void setShadowStrength(float strength);

    float displayWidth();

    void setDisplayWidth(float width);

    float displayHeight();

    void setDisplayHeight(float height);

    int glowColorOverride();

    void setGlowColorOverride(int rgb);

    void clearGlowColorOverride();

    enum Billboard {
        FIXED,
        VERTICAL,
        HORIZONTAL,
        CENTER;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
