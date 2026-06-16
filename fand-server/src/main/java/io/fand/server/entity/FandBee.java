package io.fand.server.entity;

import io.fand.api.entity.Bee;
import io.fand.server.world.WorldRegistry;

public final class FandBee extends FandAnimal implements Bee {

    public FandBee(net.minecraft.world.entity.animal.bee.Bee handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.bee.Bee handle() {
        return (net.minecraft.world.entity.animal.bee.Bee) handle;
    }
}
