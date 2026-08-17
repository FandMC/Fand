package io.fand.server.entity;

import io.fand.api.entity.CopperGolem;
import io.fand.api.world.Location;
import io.fand.server.world.WorldRegistry;
import java.util.Optional;

public final class FandCopperGolem extends FandMob implements CopperGolem {

    public FandCopperGolem(net.minecraft.world.entity.animal.golem.CopperGolem handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.golem.CopperGolem handle() {
        return (net.minecraft.world.entity.animal.golem.CopperGolem) handle;
    }

    @Override
    public ActivityState activityState() {
        return ActivityState.valueOf(handle().getState().name());
    }

    @Override
    public void setActivityState(ActivityState state) {
        runOnServerThread(() -> handle().setState(net.minecraft.world.entity.animal.golem.CopperGolemState.valueOf(state.name())));
    }

    @Override
    public WeatherState weatherState() {
        return WeatherState.valueOf(handle().getWeatherState().name());
    }

    @Override
    public void setWeatherState(WeatherState state) {
        runOnServerThread(() -> handle().setWeatherState(net.minecraft.world.level.block.WeatheringCopper.WeatherState.valueOf(state.name())));
    }

    @Override
    public Optional<Location> openedContainer() {
        var pos = handle().fand$getOpenedChestPos();
        return pos == null ? Optional.empty() : Optional.of(new Location(world(), pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F));
    }

    @Override
    public void clearOpenedContainer() {
        runOnServerThread(handle()::clearOpenedChestPos);
    }

    @Override
    public boolean readyForShearing() {
        return handle().readyForShearing();
    }
}
