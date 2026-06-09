package io.fand.server.entity;

import io.fand.api.block.Block;
import io.fand.api.entity.Hanging;
import io.fand.server.block.FandBlock;
import io.fand.server.world.FandWorld;
import io.fand.server.world.WorldRegistry;

public final class FandHanging extends FandEntity implements Hanging {

    public FandHanging(net.minecraft.world.entity.decoration.BlockAttachedEntity handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.decoration.BlockAttachedEntity handle() {
        return (net.minecraft.world.entity.decoration.BlockAttachedEntity) handle;
    }

    @Override
    public Block attachedBlock() {
        var pos = handle().getPos();
        return new FandBlock((FandWorld) world(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public boolean survives() {
        return handle().survives();
    }
}
