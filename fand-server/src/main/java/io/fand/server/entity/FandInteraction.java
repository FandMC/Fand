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

    public FandInteraction(net.minecraft.world.entity.Interaction handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.Interaction handle() {
        return (net.minecraft.world.entity.Interaction) handle;
    }

    @Override
    public boolean responsive() {
        return ReflectionFields.booleanValue(GET_RESPONSE, handle());
    }

    @Override
    public void setResponsive(boolean responsive) {
        runOnServerThread(() -> ReflectionFields.setBoolean(SET_RESPONSE, handle(), responsive));
    }
}
