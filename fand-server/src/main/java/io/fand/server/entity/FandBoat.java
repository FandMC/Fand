package io.fand.server.entity;

import io.fand.api.entity.Boat;
import io.fand.server.world.WorldRegistry;

public final class FandBoat extends FandVehicle implements Boat {

    public FandBoat(net.minecraft.world.entity.vehicle.boat.AbstractBoat handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.vehicle.boat.AbstractBoat handle() {
        return (net.minecraft.world.entity.vehicle.boat.AbstractBoat) handle;
    }
}
