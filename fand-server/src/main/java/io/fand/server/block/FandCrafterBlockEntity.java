package io.fand.server.block;

import io.fand.api.block.CrafterBlockEntity;

public final class FandCrafterBlockEntity extends FandContainerBlockEntity implements CrafterBlockEntity {
    public FandCrafterBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.CrafterBlockEntity handle) {
        super(block, handle, handle);
    }

    @Override public net.minecraft.world.level.block.entity.CrafterBlockEntity handle() { return (net.minecraft.world.level.block.entity.CrafterBlockEntity) super.handle(); }
    @Override
    public boolean slotEnabled(int slot) {
        checkSlot(slot);
        return block.callOnServerThread(() -> !handle().isSlotDisabled(slot));
    }

    @Override
    public void setSlotEnabled(int slot, boolean enabled) {
        checkSlot(slot);
        block.runOnServerThread(() -> handle().setSlotState(slot, enabled));
    }

    @Override public boolean triggered() { return block.callOnServerThread(() -> handle().isTriggered()); }

    @Override
    public void setTriggered(boolean triggered) {
        block.runOnServerThread(() -> {
            var state = block.worldHandle().getBlockState(block.position());
            if (state.hasProperty(net.minecraft.world.level.block.CrafterBlock.TRIGGERED)
                    && !block.worldHandle().setBlock(
                    block.position(),
                    state.setValue(net.minecraft.world.level.block.CrafterBlock.TRIGGERED, triggered),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS)) {
                return;
            }
            handle().setTriggered(triggered);
            syncBlockEntity();
        });
    }

    @Override public int redstoneSignal() { return block.callOnServerThread(() -> handle().getRedstoneSignal()); }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= 9) {
            throw new IndexOutOfBoundsException("slot: " + slot);
        }
    }
}
