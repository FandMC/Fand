package io.fand.server.entity;

import io.fand.api.entity.Animal;
import io.fand.server.world.WorldRegistry;

public class FandAnimal extends FandAgeable implements Animal {

    public FandAnimal(net.minecraft.world.entity.animal.Animal handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.Animal handle() {
        return (net.minecraft.world.entity.animal.Animal) handle;
    }
}
