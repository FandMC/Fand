package io.fand.server.block;

import io.fand.api.block.BrewingStandBlockEntity;
import io.fand.server.util.ReflectionFields;
import java.lang.reflect.Field;

public final class FandBrewingStandBlockEntity extends FandContainerBlockEntity implements BrewingStandBlockEntity {

    private static final Field BREW_TIME = ReflectionFields.field(
            net.minecraft.world.level.block.entity.BrewingStandBlockEntity.class, "brewTime");
    private static final Field FUEL = ReflectionFields.field(
            net.minecraft.world.level.block.entity.BrewingStandBlockEntity.class, "fuel");

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

    @Override
    public void setBrewTime(int ticks) {
        updateBrewingStand(() -> ReflectionFields.setInt(BREW_TIME, handle(), Math.max(0, ticks)));
    }

    @Override
    public int fuel() {
        return ReflectionFields.intValue(FUEL, handle());
    }

    @Override
    public void setFuel(int fuel) {
        updateBrewingStand(() -> ReflectionFields.setInt(FUEL, handle(), Math.max(0, fuel)));
    }

    private void updateBrewingStand(Runnable update) {
        block.runOnServerThread(() -> {
            update.run();
            handle().setChanged();
            var state = block.worldHandle().getBlockState(block.position());
            block.worldHandle().sendBlockUpdated(block.position(), state, state, net.minecraft.world.level.block.Block.UPDATE_NONE);
        });
    }
}
