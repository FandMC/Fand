package io.fand.server.entity;

import io.fand.api.block.BlockType;
import io.fand.api.entity.Minecart;
import io.fand.server.block.FandBlockType;
import io.fand.server.world.WorldRegistry;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class FandMinecart extends FandVehicle implements Minecart {

    public FandMinecart(net.minecraft.world.entity.vehicle.minecart.AbstractMinecart handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.vehicle.minecart.AbstractMinecart handle() {
        return (net.minecraft.world.entity.vehicle.minecart.AbstractMinecart) handle;
    }

    @Override
    public boolean onRails() {
        return handle().isOnRails();
    }

    @Override
    public boolean flipped() {
        return handle().isFlipped();
    }

    @Override
    public void setFlipped(boolean flipped) {
        runOnServerThread(() -> handle().setFlipped(flipped));
    }

    @Override
    public Optional<? extends BlockType> customDisplayBlock() {
        var state = handle().getDisplayBlockState();
        return state.isAir() ? Optional.empty() : Optional.of(FandBlockType.of(state.getBlock()));
    }

    @Override
    public void setCustomDisplayBlock(@Nullable BlockType type) {
        runOnServerThread(() -> handle().setCustomDisplayBlockState(type == null
                ? Optional.empty()
                : Optional.of(FandBlockType.unwrap(type).defaultBlockState())));
    }

    @Override
    public int displayOffset() {
        return handle().getDisplayOffset();
    }

    @Override
    public void setDisplayOffset(int offset) {
        runOnServerThread(() -> handle().setDisplayOffset(offset));
    }

    @Override
    public boolean rideable() {
        return handle().isRideable();
    }

    @Override
    public boolean furnace() {
        return handle().isFurnace();
    }
}
