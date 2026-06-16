package io.fand.server.entity;

import io.fand.api.block.BlockType;
import io.fand.api.entity.FallingBlock;
import io.fand.server.block.FandBlockType;
import io.fand.server.world.WorldRegistry;

public final class FandFallingBlock extends FandEntity implements FallingBlock {

    public FandFallingBlock(net.minecraft.world.entity.item.FallingBlockEntity handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.item.FallingBlockEntity handle() {
        return (net.minecraft.world.entity.item.FallingBlockEntity) handle;
    }

    @Override
    public BlockType blockType() {
        return FandBlockType.of(handle().getBlockState().getBlock());
    }
}
