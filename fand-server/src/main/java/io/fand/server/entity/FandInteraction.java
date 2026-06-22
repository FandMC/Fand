package io.fand.server.entity;

import io.fand.api.entity.Interaction;
import io.fand.server.util.ReflectionFields;
import io.fand.server.world.WorldRegistry;
import java.lang.reflect.Method;

public final class FandInteraction extends FandEntity implements Interaction {

    private static final Method GET_RESPONSE = ReflectionFields.method(
            net.minecraft.world.entity.Interaction.class, "getResponse");
    private static final Method SET_RESPONSE = ReflectionFields.method(
            net.minecraft.world.entity.Interaction.class, "setResponse", boolean.class);
    private static final Method GET_WIDTH = ReflectionFields.method(
            net.minecraft.world.entity.Interaction.class, "getWidth");
    private static final Method SET_WIDTH = ReflectionFields.method(
            net.minecraft.world.entity.Interaction.class, "setWidth", float.class);
    private static final Method GET_HEIGHT = ReflectionFields.method(
            net.minecraft.world.entity.Interaction.class, "getHeight");
    private static final Method SET_HEIGHT = ReflectionFields.method(
            net.minecraft.world.entity.Interaction.class, "setHeight", float.class);

    public FandInteraction(net.minecraft.world.entity.Interaction handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.Interaction handle() {
        return (net.minecraft.world.entity.Interaction) handle;
    }

    @Override
    public float interactionWidth() {
        return ReflectionFields.call(GET_WIDTH, handle(), Float.class);
    }

    @Override
    public void setInteractionWidth(float width) {
        requireFinitePositive(width, "width");
        runOnServerThread(() -> ReflectionFields.invoke(SET_WIDTH, handle(), width));
    }

    @Override
    public float interactionHeight() {
        return ReflectionFields.call(GET_HEIGHT, handle(), Float.class);
    }

    @Override
    public void setInteractionHeight(float height) {
        requireFinitePositive(height, "height");
        runOnServerThread(() -> ReflectionFields.invoke(SET_HEIGHT, handle(), height));
    }

    @Override
    public boolean responsive() {
        return ReflectionFields.booleanValue(GET_RESPONSE, handle());
    }

    @Override
    public void setResponsive(boolean responsive) {
        runOnServerThread(() -> ReflectionFields.setBoolean(SET_RESPONSE, handle(), responsive));
    }

    private static void requireFinitePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and > 0");
        }
    }
}
