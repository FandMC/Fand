package io.fand.server.entity;

import io.fand.api.entity.Display;
import io.fand.server.world.WorldRegistry;
import java.util.Objects;

public class FandDisplay extends FandEntity implements Display {

    public FandDisplay(net.minecraft.world.entity.Display handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.Display handle() {
        return (net.minecraft.world.entity.Display) handle;
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
}
