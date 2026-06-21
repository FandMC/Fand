package io.fand.server.entity;

import io.fand.api.entity.Boat;
import io.fand.server.util.ReflectionFields;
import io.fand.server.world.WorldRegistry;
import java.lang.reflect.Method;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.BuiltInRegistries;

public final class FandBoat extends FandVehicle implements Boat {
    private static final Method GET_BUBBLE_TIME = ReflectionFields.method(
            net.minecraft.world.entity.vehicle.boat.AbstractBoat.class,
            "getBubbleTime");
    private static final Method SET_BUBBLE_TIME = ReflectionFields.method(
            net.minecraft.world.entity.vehicle.boat.AbstractBoat.class,
            "setBubbleTime",
            int.class);

    public FandBoat(net.minecraft.world.entity.vehicle.boat.AbstractBoat handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.vehicle.boat.AbstractBoat handle() {
        return (net.minecraft.world.entity.vehicle.boat.AbstractBoat) handle;
    }

    @Override
    public Key woodType() {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(handle().getType());
        var path = id == null ? "oak" : id.getPath();
        path = path
                .replace("_chest_boat", "")
                .replace("_boat", "")
                .replace("_chest_raft", "")
                .replace("_raft", "");
        return Key.key("minecraft", path);
    }

    @Override
    public boolean chestBoat() {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(handle().getType());
        return id != null && id.getPath().contains("_chest_");
    }

    @Override
    public boolean raft() {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(handle().getType());
        return id != null && id.getPath().endsWith("_raft");
    }

    @Override
    public boolean paddling(Paddle paddle) {
        java.util.Objects.requireNonNull(paddle, "paddle");
        return handle().getPaddleState(paddle == Paddle.LEFT
                ? net.minecraft.world.entity.vehicle.boat.AbstractBoat.PADDLE_LEFT
                : net.minecraft.world.entity.vehicle.boat.AbstractBoat.PADDLE_RIGHT);
    }

    @Override
    public void setPaddling(boolean left, boolean right) {
        runOnServerThread(() -> handle().setPaddleState(left, right));
    }

    @Override
    public int bubbleTime() {
        return ReflectionFields.call(GET_BUBBLE_TIME, handle(), Integer.class);
    }

    @Override
    public void setBubbleTime(int ticks) {
        runOnServerThread(() -> ReflectionFields.invoke(SET_BUBBLE_TIME, handle(), Math.max(0, ticks)));
    }
}
