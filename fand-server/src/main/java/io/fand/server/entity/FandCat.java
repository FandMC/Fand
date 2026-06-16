package io.fand.server.entity;

import io.fand.api.entity.Cat;
import io.fand.server.world.WorldRegistry;

public final class FandCat extends FandTameable implements Cat {

    public FandCat(net.minecraft.world.entity.animal.feline.Cat handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.feline.Cat handle() {
        return (net.minecraft.world.entity.animal.feline.Cat) handle;
    }
}
