package io.fand.server.entity;

import io.fand.api.entity.Horse;
import io.fand.api.entity.LivingEntity;
import io.fand.server.world.WorldRegistry;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class FandHorse extends FandAnimal implements Horse {

    public FandHorse(net.minecraft.world.entity.animal.equine.AbstractHorse handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.equine.AbstractHorse handle() {
        return (net.minecraft.world.entity.animal.equine.AbstractHorse) handle;
    }

    @Override
    public boolean tamed() {
        return handle().isTamed();
    }

    @Override
    public void setTamed(boolean tamed) {
        runOnServerThread(() -> handle().setTamed(tamed));
    }

    @Override
    public Optional<? extends LivingEntity> owner() {
        var owner = handle().getOwner();
        return owner == null ? Optional.empty() : Optional.of(worldRegistry.entityRegistry().wrap(owner));
    }

    @Override
    public void setOwner(@Nullable LivingEntity owner) {
        runOnServerThread(() -> handle().setOwner(owner == null
                ? null
                : (net.minecraft.world.entity.LivingEntity) EntityHandles.unwrap(owner)));
    }

    @Override
    public int temper() {
        return handle().getTemper();
    }

    @Override
    public void setTemper(int temper) {
        runOnServerThread(() -> handle().setTemper(Math.max(0, Math.min(handle().getMaxTemper(), temper))));
    }

    @Override
    public int maxTemper() {
        return handle().getMaxTemper();
    }

    @Override
    public boolean bred() {
        return handle().isBred();
    }

    @Override
    public void setBred(boolean bred) {
        runOnServerThread(() -> handle().setBred(bred));
    }

    @Override
    public boolean eating() {
        return handle().isEating();
    }

    @Override
    public void setEating(boolean eating) {
        runOnServerThread(() -> handle().setEating(eating));
    }

    @Override
    public boolean standing() {
        return handle().isStanding();
    }

    @Override
    public void stand() {
        runOnServerThread(handle()::standIfPossible);
    }
}
