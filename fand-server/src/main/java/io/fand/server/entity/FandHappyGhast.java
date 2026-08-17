package io.fand.server.entity;

import io.fand.api.entity.HappyGhast;
import io.fand.server.world.WorldRegistry;

public final class FandHappyGhast extends FandAnimal implements HappyGhast {

    public FandHappyGhast(net.minecraft.world.entity.animal.happyghast.HappyGhast handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.happyghast.HappyGhast handle() {
        return (net.minecraft.world.entity.animal.happyghast.HappyGhast) handle;
    }

    @Override public boolean leashHolder() { return handle().isLeashHolder(); }
    @Override public boolean staysStill() { return handle().staysStill(); }
    @Override public boolean onStillTimeout() { return handle().isOnStillTimeout(); }

    @Override
    public void setStillTimeoutTicks(int ticks) {
        if (ticks < 0) throw new IllegalArgumentException("ticks must be non-negative");
        runOnServerThread(() -> handle().fand$setStillTimeout(ticks));
    }
}
