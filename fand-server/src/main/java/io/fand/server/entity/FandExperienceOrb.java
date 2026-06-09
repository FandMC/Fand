package io.fand.server.entity;

import io.fand.api.entity.ExperienceOrb;
import io.fand.server.world.WorldRegistry;

public final class FandExperienceOrb extends FandEntity implements ExperienceOrb {

    public FandExperienceOrb(net.minecraft.world.entity.ExperienceOrb handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.ExperienceOrb handle() {
        return (net.minecraft.world.entity.ExperienceOrb) handle;
    }

    @Override
    public int experience() {
        return handle().getValue();
    }

    @Override
    public void setExperience(int experience) {
        runOnServerThread(() -> handle().fand$setValue(Math.max(0, experience)));
    }

    @Override
    public int icon() {
        return handle().getIcon();
    }

    @Override
    public int age() {
        return handle().fand$age();
    }

    @Override
    public void setAge(int ticks) {
        runOnServerThread(() -> handle().fand$setAge(Math.max(0, ticks)));
    }

    @Override
    public int count() {
        return handle().fand$count();
    }

    @Override
    public void setCount(int count) {
        runOnServerThread(() -> handle().fand$setCount(count));
    }
}
