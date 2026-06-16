package io.fand.server.entity;

import io.fand.api.entity.Wolf;
import io.fand.server.world.WorldRegistry;

public final class FandWolf extends FandTameable implements Wolf {

    public FandWolf(net.minecraft.world.entity.animal.wolf.Wolf handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.wolf.Wolf handle() {
        return (net.minecraft.world.entity.animal.wolf.Wolf) handle;
    }
}
