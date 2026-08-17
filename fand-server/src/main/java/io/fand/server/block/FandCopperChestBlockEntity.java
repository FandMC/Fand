package io.fand.server.block;

import io.fand.api.block.CopperChestBlockEntity;

public final class FandCopperChestBlockEntity extends FandContainerBlockEntity implements CopperChestBlockEntity {
    public FandCopperChestBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.ChestBlockEntity handle) {
        super(block, handle, handle);
    }
}
