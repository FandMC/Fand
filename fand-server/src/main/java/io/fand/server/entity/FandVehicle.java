package io.fand.server.entity;

import io.fand.api.entity.Vehicle;
import io.fand.server.world.WorldRegistry;

public class FandVehicle extends FandEntity implements Vehicle {

    public FandVehicle(net.minecraft.world.entity.vehicle.VehicleEntity handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.vehicle.VehicleEntity handle() {
        return (net.minecraft.world.entity.vehicle.VehicleEntity) handle;
    }

    @Override
    public double damage() {
        return handle().getDamage();
    }

    @Override
    public void setDamage(double damage) {
        runOnServerThread(() -> handle().setDamage((float) Math.max(0.0, damage)));
    }

    @Override
    public int hurtTime() {
        return handle().getHurtTime();
    }

    @Override
    public void setHurtTime(int ticks) {
        runOnServerThread(() -> handle().setHurtTime(Math.max(0, ticks)));
    }

    @Override
    public int hurtDirection() {
        return handle().getHurtDir();
    }

    @Override
    public void setHurtDirection(int direction) {
        runOnServerThread(() -> handle().setHurtDir(direction));
    }
}
