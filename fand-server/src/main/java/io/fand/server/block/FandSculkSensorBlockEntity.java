package io.fand.server.block;

import io.fand.api.block.SculkSensorBlockEntity;

public final class FandSculkSensorBlockEntity extends FandBlockEntity implements SculkSensorBlockEntity {

    public FandSculkSensorBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.SculkSensorBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.SculkSensorBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.SculkSensorBlockEntity) handle;
    }

    @Override
    public int lastVibrationFrequency() {
        return handle().getLastVibrationFrequency();
    }
}
