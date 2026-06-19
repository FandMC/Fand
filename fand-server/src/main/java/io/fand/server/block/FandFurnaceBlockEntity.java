package io.fand.server.block;

import io.fand.api.block.FurnaceBlockEntity;
import io.fand.server.util.ReflectionFields;
import java.lang.reflect.Field;
import net.minecraft.world.level.block.AbstractFurnaceBlock;

public final class FandFurnaceBlockEntity extends FandContainerBlockEntity implements FurnaceBlockEntity {

    private static final Field COOKING_TIMER = ReflectionFields.field(
            net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class, "cookingTimer");
    private static final Field COOKING_TOTAL_TIME = ReflectionFields.field(
            net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class, "cookingTotalTime");
    private static final Field LIT_TIME_REMAINING = ReflectionFields.field(
            net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class, "litTimeRemaining");
    private static final Field LIT_TOTAL_TIME = ReflectionFields.field(
            net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class, "litTotalTime");

    public FandFurnaceBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity handle) {
        super(block, handle, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) handle;
    }

    @Override
    public int cookTime() {
        return ReflectionFields.intValue(COOKING_TIMER, handle());
    }

    @Override
    public void setCookTime(int ticks) {
        updateFurnace(() -> ReflectionFields.setInt(COOKING_TIMER, handle(), Math.max(0, ticks)), false);
    }

    @Override
    public int cookTimeTotal() {
        return ReflectionFields.intValue(COOKING_TOTAL_TIME, handle());
    }

    @Override
    public void setCookTimeTotal(int ticks) {
        updateFurnace(() -> ReflectionFields.setInt(COOKING_TOTAL_TIME, handle(), Math.max(0, ticks)), false);
    }

    @Override
    public int burnTime() {
        return ReflectionFields.intValue(LIT_TIME_REMAINING, handle());
    }

    @Override
    public void setBurnTime(int ticks) {
        updateFurnace(() -> ReflectionFields.setInt(LIT_TIME_REMAINING, handle(), Math.max(0, ticks)), true);
    }

    @Override
    public int burnTimeTotal() {
        return ReflectionFields.intValue(LIT_TOTAL_TIME, handle());
    }

    @Override
    public void setBurnTimeTotal(int ticks) {
        updateFurnace(() -> ReflectionFields.setInt(LIT_TOTAL_TIME, handle(), Math.max(0, ticks)), false);
    }

    private void updateFurnace(Runnable update, boolean updateLitState) {
        block.runOnServerThread(() -> {
            update.run();
            handle().setChanged();
            if (updateLitState) {
                syncLitState();
            } else {
                sendBlockUpdate();
            }
        });
    }

    private void syncLitState() {
        var state = block.worldHandle().getBlockState(block.position());
        if (state.hasProperty(AbstractFurnaceBlock.LIT)) {
            var next = state.setValue(AbstractFurnaceBlock.LIT, burnTime() > 0);
            if (next != state) {
                block.worldHandle().setBlock(block.position(), next, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                return;
            }
        }
        sendBlockUpdate();
    }

    private void sendBlockUpdate() {
        var state = block.worldHandle().getBlockState(block.position());
        block.worldHandle().sendBlockUpdated(block.position(), state, state, net.minecraft.world.level.block.Block.UPDATE_NONE);
    }
}
