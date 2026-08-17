package io.fand.server.block;

import io.fand.api.block.ShelfBlockEntity;

public final class FandShelfBlockEntity extends FandContainerBlockEntity implements ShelfBlockEntity {
    public FandShelfBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.ShelfBlockEntity handle) {
        super(block, handle, handle);
    }

    @Override public net.minecraft.world.level.block.entity.ShelfBlockEntity handle() { return (net.minecraft.world.level.block.entity.ShelfBlockEntity) super.handle(); }
    @Override public boolean alignItemsToBottom() { return block.callOnServerThread(() -> handle().getAlignItemsToBottom()); }

    @Override
    public void setAlignItemsToBottom(boolean align) {
        block.runOnServerThread(() -> {
            handle().fand$setAlignItemsToBottom(align);
            syncBlockEntity();
        });
    }
}
