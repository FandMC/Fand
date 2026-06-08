package io.fand.server.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.Tameable;
import io.fand.server.world.WorldRegistry;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class FandTameable extends FandAnimal implements Tameable {

    public FandTameable(net.minecraft.world.entity.TamableAnimal handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.TamableAnimal handle() {
        return (net.minecraft.world.entity.TamableAnimal) handle;
    }

    @Override
    public boolean tamed() {
        return handle().isTame();
    }

    @Override
    public void setTamed(boolean tamed) {
        runOnServerThread(() -> handle().setTame(tamed, true));
    }

    @Override
    public Optional<? extends LivingEntity> owner() {
        var owner = handle().getOwner();
        if (owner == null) {
            return Optional.empty();
        }
        return Optional.of(worldRegistry.entityRegistry().wrap(owner));
    }

    @Override
    public void setOwner(@Nullable LivingEntity owner) {
        runOnServerThread(() -> handle().setOwner(owner == null
                ? null
                : (net.minecraft.world.entity.LivingEntity) EntityHandles.unwrap(owner)));
    }

    @Override
    public boolean sitting() {
        return handle().isInSittingPose();
    }

    @Override
    public void setSitting(boolean sitting) {
        runOnServerThread(() -> {
            handle().setInSittingPose(sitting);
            handle().setOrderedToSit(sitting);
        });
    }
}
