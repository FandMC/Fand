package io.fand.server.entity;

import io.fand.api.entity.Warden;
import io.fand.server.world.WorldRegistry;

public final class FandWarden extends FandMob implements Warden {

    public FandWarden(net.minecraft.world.entity.monster.warden.Warden handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.monster.warden.Warden handle() {
        return (net.minecraft.world.entity.monster.warden.Warden) handle;
    }
}
