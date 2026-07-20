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

    @Override
    public boolean canBreed() {
        return !handle().isBaby() && handle().canFallInLove();
    }

    @Override
    public boolean inLove() {
        return handle().isInLove();
    }

    @Override
    public int loveTicks() {
        return handle().getInLoveTime();
    }

    @Override
    public void setLoveTicks(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks must be non-negative");
        }
        runOnServerThread(() -> handle().setInLoveTime(ticks));
    }
}
