package io.fand.server.block;

import io.fand.api.block.FurnaceBlockEntity;
import io.fand.server.util.ReflectionFields;
import java.lang.reflect.Field;

public final class FandFurnaceBlockEntity extends FandContainerBlockEntity implements FurnaceBlockEntity {

    private static final Field COOKING_TIMER = ReflectionFields.field(
            net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class, "cookingTimer");
    private static final Field LIT_TIME_REMAINING = ReflectionFields.field(
            net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class, "litTimeRemaining");

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
    public int burnTime() {
        return ReflectionFields.intValue(LIT_TIME_REMAINING, handle());
    }
}
