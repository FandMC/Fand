package io.fand.server.entity;

import com.mojang.math.Transformation;
import io.fand.api.entity.DisplayTransformation;
import io.fand.api.entity.Display;
import io.fand.api.entity.Quaternion;
import io.fand.api.world.Vector3;
import io.fand.server.util.ReflectionFields;
import io.fand.server.world.WorldRegistry;
import java.lang.reflect.Method;
import java.util.Objects;
import net.minecraft.network.syncher.SynchedEntityData;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class FandDisplay extends FandEntity implements Display {

    private static final Method CREATE_TRANSFORMATION = ReflectionFields.method(
            net.minecraft.world.entity.Display.class, "createTransformation", SynchedEntityData.class);
    private static final Method SET_TRANSFORMATION = ReflectionFields.method(
            net.minecraft.world.entity.Display.class, "setTransformation", Transformation.class);

    public FandDisplay(net.minecraft.world.entity.Display handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.Display handle() {
        return (net.minecraft.world.entity.Display) handle;
    }

    @Override
    public DisplayTransformation transformation() {
        var transformation = ReflectionFields.call(
                CREATE_TRANSFORMATION,
                null,
                Transformation.class,
                handle().getEntityData());
        return fromVanilla(transformation);
    }

    @Override
    public void setTransformation(DisplayTransformation transformation) {
        Objects.requireNonNull(transformation, "transformation");
        runOnServerThread(() -> ReflectionFields.invoke(SET_TRANSFORMATION, handle(), toVanilla(transformation)));
    }

    @Override
    public int transformationInterpolationDuration() {
        return handle().fand$transformationInterpolationDuration();
    }

    @Override
    public void setTransformationInterpolationDuration(int ticks) {
        runOnServerThread(() -> handle().fand$setTransformationInterpolationDuration(Math.max(0, ticks)));
    }

    @Override
    public int transformationInterpolationDelay() {
        return handle().fand$transformationInterpolationDelay();
    }

    @Override
    public void setTransformationInterpolationDelay(int ticks) {
        runOnServerThread(() -> handle().fand$setTransformationInterpolationDelay(ticks));
    }

    @Override
    public int positionRotationInterpolationDuration() {
        return handle().fand$posRotInterpolationDuration();
    }

    @Override
    public void setPositionRotationInterpolationDuration(int ticks) {
        runOnServerThread(() -> handle().fand$setPosRotInterpolationDuration(Math.clamp(ticks, 0, 59)));
    }

    @Override
    public Billboard billboard() {
        return fromVanilla(handle().fand$billboardConstraints());
    }

    @Override
    public void setBillboard(Billboard billboard) {
        Objects.requireNonNull(billboard, "billboard");
        runOnServerThread(() -> handle().fand$setBillboardConstraints(toVanilla(billboard)));
    }

    @Override
    public int packedBrightnessOverride() {
        return handle().fand$packedBrightnessOverride();
    }

    @Override
    public void setPackedBrightnessOverride(int brightness) {
        runOnServerThread(() -> handle().fand$setPackedBrightnessOverride(brightness));
    }

    @Override
    public void clearBrightnessOverride() {
        setPackedBrightnessOverride(-1);
    }

    @Override
    public float viewRange() {
        return handle().fand$viewRange();
    }

    @Override
    public void setViewRange(float range) {
        requireFinite(range, "range");
        runOnServerThread(() -> handle().fand$setViewRange(Math.max(0.0F, range)));
    }

    @Override
    public float shadowRadius() {
        return handle().fand$shadowRadius();
    }

    @Override
    public void setShadowRadius(float radius) {
        requireFinite(radius, "radius");
        runOnServerThread(() -> handle().fand$setShadowRadius(Math.max(0.0F, radius)));
    }

    @Override
    public float shadowStrength() {
        return handle().fand$shadowStrength();
    }

    @Override
    public void setShadowStrength(float strength) {
        requireFinite(strength, "strength");
        runOnServerThread(() -> handle().fand$setShadowStrength(Math.clamp(strength, 0.0F, 1.0F)));
    }

    @Override
    public float displayWidth() {
        return handle().fand$displayWidth();
    }

    @Override
    public void setDisplayWidth(float width) {
        requireFinite(width, "width");
        runOnServerThread(() -> handle().fand$setDisplayWidth(Math.max(0.0F, width)));
    }

    @Override
    public float displayHeight() {
        return handle().fand$displayHeight();
    }

    @Override
    public void setDisplayHeight(float height) {
        requireFinite(height, "height");
        runOnServerThread(() -> handle().fand$setDisplayHeight(Math.max(0.0F, height)));
    }

    @Override
    public int glowColorOverride() {
        return handle().fand$glowColorOverride();
    }

    @Override
    public void setGlowColorOverride(int rgb) {
        runOnServerThread(() -> handle().fand$setGlowColorOverride(rgb));
    }

    @Override
    public void clearGlowColorOverride() {
        setGlowColorOverride(-1);
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static Billboard fromVanilla(net.minecraft.world.entity.Display.BillboardConstraints constraints) {
        return switch (constraints) {
            case FIXED -> Billboard.FIXED;
            case VERTICAL -> Billboard.VERTICAL;
            case HORIZONTAL -> Billboard.HORIZONTAL;
            case CENTER -> Billboard.CENTER;
        };
    }

    private static net.minecraft.world.entity.Display.BillboardConstraints toVanilla(Billboard billboard) {
        return switch (billboard) {
            case FIXED -> net.minecraft.world.entity.Display.BillboardConstraints.FIXED;
            case VERTICAL -> net.minecraft.world.entity.Display.BillboardConstraints.VERTICAL;
            case HORIZONTAL -> net.minecraft.world.entity.Display.BillboardConstraints.HORIZONTAL;
            case CENTER -> net.minecraft.world.entity.Display.BillboardConstraints.CENTER;
        };
    }

    private static DisplayTransformation fromVanilla(Transformation transformation) {
        return new DisplayTransformation(
                vector(transformation.translation()),
                quaternion(transformation.leftRotation()),
                vector(transformation.scale()),
                quaternion(transformation.rightRotation()));
    }

    private static Transformation toVanilla(DisplayTransformation transformation) {
        return new Transformation(
                vector(transformation.translation()),
                quaternion(transformation.leftRotation()),
                vector(transformation.scale()),
                quaternion(transformation.rightRotation()));
    }

    private static Vector3 vector(Vector3fc vector) {
        return new Vector3(vector.x(), vector.y(), vector.z());
    }

    private static Vector3f vector(Vector3 vector) {
        return new Vector3f((float) vector.x(), (float) vector.y(), (float) vector.z());
    }

    private static Quaternion quaternion(Quaternionfc quaternion) {
        return new Quaternion(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w());
    }

    private static Quaternionf quaternion(Quaternion quaternion) {
        return new Quaternionf(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w());
    }
}
