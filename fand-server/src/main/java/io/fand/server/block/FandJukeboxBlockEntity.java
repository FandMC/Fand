package io.fand.server.block;

import io.fand.api.block.JukeboxBlockEntity;
import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import java.util.Objects;

public final class FandJukeboxBlockEntity extends FandBlockEntity implements JukeboxBlockEntity {

    public FandJukeboxBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.JukeboxBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.JukeboxBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.JukeboxBlockEntity) handle;
    }

    @Override
    public ItemStack record() {
        return FandItemStacks.fromVanilla(handle().getTheItem());
    }

    @Override
    public void setRecord(ItemStack record) {
        Objects.requireNonNull(record, "record");
        block.runOnServerThread(() -> handle().setTheItem(FandItemStacks.toVanilla(record)));
    }
}
