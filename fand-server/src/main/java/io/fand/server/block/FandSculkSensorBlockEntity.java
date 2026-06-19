package io.fand.server.block;

import io.fand.api.block.SculkSensorBlockEntity;
import io.fand.api.block.SculkSensorPhase;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;

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

    @Override
    public void setLastVibrationFrequency(int frequency) {
        block.runOnServerThread(() -> {
            handle().setLastVibrationFrequency(Math.max(0, frequency));
            syncBlockEntity();
        });
    }

    @Override
    public SculkSensorPhase phase() {
        var state = block.worldHandle().getBlockState(block.position());
        if (!state.hasProperty(SculkSensorBlock.PHASE)) {
            return SculkSensorPhase.INACTIVE;
        }
        return switch (state.getValue(SculkSensorBlock.PHASE)) {
            case ACTIVE -> SculkSensorPhase.ACTIVE;
            case COOLDOWN -> SculkSensorPhase.COOLDOWN;
            case INACTIVE -> SculkSensorPhase.INACTIVE;
        };
    }

    @Override
    public int power() {
        var state = block.worldHandle().getBlockState(block.position());
        return state.hasProperty(SculkSensorBlock.POWER) ? state.getValue(SculkSensorBlock.POWER) : 0;
    }

    @Override
    public void activate(int power, int vibrationFrequency) {
        block.runOnServerThread(() -> {
            var state = block.worldHandle().getBlockState(block.position());
            if (state.getBlock() instanceof SculkSensorBlock sculk) {
                int clampedPower = Math.max(0, Math.min(15, power));
                int clampedFrequency = Math.max(0, Math.min(15, vibrationFrequency));
                handle().setLastVibrationFrequency(clampedFrequency);
                sculk.activate(null, block.worldHandle(), block.position(), state, clampedPower, clampedFrequency);
            }
        });
    }

    @Override
    public void deactivate() {
        block.runOnServerThread(() -> {
            BlockState state = block.worldHandle().getBlockState(block.position());
            if (state.getBlock() instanceof SculkSensorBlock) {
                SculkSensorBlock.deactivate(block.worldHandle(), block.position(), state);
            }
        });
    }
}
