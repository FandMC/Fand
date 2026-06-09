package io.fand.server.entity;

import io.fand.api.block.BlockType;
import io.fand.api.entity.Enderman;
import io.fand.server.block.FandBlockType;
import io.fand.server.world.WorldRegistry;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class FandEnderman extends FandMob implements Enderman {

    public FandEnderman(net.minecraft.world.entity.monster.EnderMan handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.monster.EnderMan handle() {
        return (net.minecraft.world.entity.monster.EnderMan) handle;
    }

    @Override
    public Optional<? extends BlockType> carriedBlock() {
        var state = handle().getCarriedBlock();
        return state == null ? Optional.empty() : Optional.of(FandBlockType.of(state.getBlock()));
    }

    @Override
    public void setCarriedBlock(@Nullable BlockType type) {
        runOnServerThread(() -> handle().setCarriedBlock(type == null
                ? null
                : FandBlockType.unwrap(type).defaultBlockState()));
    }
}
