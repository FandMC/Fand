package io.fand.server.entity;

import io.fand.api.entity.Parched;
import io.fand.server.world.WorldRegistry;

public final class FandParched extends FandMob implements Parched {
    public FandParched(net.minecraft.world.entity.monster.skeleton.Parched handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.monster.skeleton.Parched handle() {
        return (net.minecraft.world.entity.monster.skeleton.Parched) handle;
    }
}
