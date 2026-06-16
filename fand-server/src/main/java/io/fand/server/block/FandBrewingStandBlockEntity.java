package io.fand.server.block;

import io.fand.api.block.BrewingStandBlockEntity;
import io.fand.server.util.ReflectionFields;
import java.lang.reflect.Field;

public final class FandBrewingStandBlockEntity extends FandContainerBlockEntity implements BrewingStandBlockEntity {

    private static final Field BREW_TIME = ReflectionFields.field(
            net.minecraft.world.level.block.entity.BrewingStandBlockEntity.class, "brewTime");

    public FandBrewingStandBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.BrewingStandBlockEntity handle) {
        super(block, handle, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.BrewingStandBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.BrewingStandBlockEntity) handle;
    }

    @Override
    public int brewTime() {
        return ReflectionFields.intValue(BREW_TIME, handle());
    }
}
