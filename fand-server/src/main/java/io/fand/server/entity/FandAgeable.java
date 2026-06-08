package io.fand.server.entity;

import io.fand.api.entity.Ageable;
import io.fand.server.world.WorldRegistry;

public class FandAgeable extends FandMob implements Ageable {

    public FandAgeable(net.minecraft.world.entity.AgeableMob handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.AgeableMob handle() {
        return (net.minecraft.world.entity.AgeableMob) handle;
    }

    @Override
    public int age() {
        return handle().getAge();
    }

    @Override
    public void setAge(int age) {
        runOnServerThread(() -> handle().setAge(age));
    }

    @Override
    public boolean baby() {
        return handle().isBaby();
    }

    @Override
    public void setBaby(boolean baby) {
        runOnServerThread(() -> handle().setBaby(baby));
    }
}
