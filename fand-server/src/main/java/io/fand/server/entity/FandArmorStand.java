package io.fand.server.entity;

import io.fand.api.entity.ArmorStand;
import io.fand.server.world.WorldRegistry;

public final class FandArmorStand extends FandLivingEntity implements ArmorStand {

    public FandArmorStand(net.minecraft.world.entity.decoration.ArmorStand handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.decoration.ArmorStand handle() {
        return (net.minecraft.world.entity.decoration.ArmorStand) handle;
    }

    @Override
    public boolean small() {
        return handle().isSmall();
    }

    @Override
    public void setSmall(boolean small) {
        runOnServerThread(() -> handle().fand$setSmall(small));
    }

    @Override
    public boolean armsVisible() {
        return handle().showArms();
    }

    @Override
    public void setArmsVisible(boolean visible) {
        runOnServerThread(() -> handle().setShowArms(visible));
    }

    @Override
    public boolean basePlateVisible() {
        return handle().showBasePlate();
    }

    @Override
    public void setBasePlateVisible(boolean visible) {
        runOnServerThread(() -> handle().setNoBasePlate(!visible));
    }

    @Override
    public boolean marker() {
        return handle().isMarker();
    }

    @Override
    public void setMarker(boolean marker) {
        runOnServerThread(() -> handle().fand$setMarker(marker));
    }

    @Override
    public boolean invisible() {
        return handle().isInvisible();
    }

    @Override
    public void setInvisible(boolean invisible) {
        runOnServerThread(() -> handle().setInvisible(invisible));
    }
}
