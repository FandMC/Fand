package io.fand.server.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockEntity;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.BuiltInRegistries;

public class FandBlockEntity implements BlockEntity {

    protected final FandBlock block;
    protected final net.minecraft.world.level.block.entity.BlockEntity handle;

    public FandBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.BlockEntity handle) {
        this.block = block;
        this.handle = handle;
    }

    public net.minecraft.world.level.block.entity.BlockEntity handle() {
        return handle;
    }

    @Override
    public Block block() {
        return block;
    }

    @Override
    public Key type() {
        var id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(handle.getType());
        return Key.key(id.getNamespace(), id.getPath());
    }

    @Override
    public boolean removed() {
        return handle.isRemoved();
    }

    protected void syncBlockEntity() {
        handle.setChanged();
        var state = block.worldHandle().getBlockState(block.position());
        block.worldHandle().sendBlockUpdated(block.position(), state, state, net.minecraft.world.level.block.Block.UPDATE_NONE);
    }
}
