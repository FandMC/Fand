package io.fand.server.entity;

import io.fand.api.entity.AreaEffectCloud;
import io.fand.api.entity.LivingEntity;
import io.fand.server.world.WorldRegistry;
import java.util.Objects;
import java.util.Optional;

public final class FandAreaEffectCloud extends FandEntity implements AreaEffectCloud {

    public FandAreaEffectCloud(net.minecraft.world.entity.AreaEffectCloud handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.AreaEffectCloud handle() {
        return (net.minecraft.world.entity.AreaEffectCloud) handle;
    }

    @Override
    public double radius() {
        return handle().getRadius();
    }

    @Override
    public void setRadius(double radius) {
        runOnServerThread(() -> handle().setRadius((float) Math.max(0.0, radius)));
    }

    @Override
    public int duration() {
        return handle().getDuration();
    }

    @Override
    public void setDuration(int ticks) {
        runOnServerThread(() -> handle().setDuration(ticks));
    }

    @Override
    public int waitTime() {
        return handle().getWaitTime();
    }

    @Override
    public void setWaitTime(int ticks) {
        runOnServerThread(() -> handle().setWaitTime(Math.max(0, ticks)));
    }

    @Override
    public double radiusOnUse() {
        return handle().getRadiusOnUse();
    }

    @Override
    public void setRadiusOnUse(double radius) {
        runOnServerThread(() -> handle().setRadiusOnUse((float) radius));
    }

    @Override
    public double radiusPerTick() {
        return handle().getRadiusPerTick();
    }

    @Override
    public void setRadiusPerTick(double radius) {
        runOnServerThread(() -> handle().setRadiusPerTick((float) radius));
    }

    @Override
    public int durationOnUse() {
        return handle().getDurationOnUse();
    }

    @Override
    public void setDurationOnUse(int ticks) {
        runOnServerThread(() -> handle().setDurationOnUse(ticks));
    }

    @Override
    public boolean waiting() {
        return handle().isWaiting();
    }

    @Override
    public Optional<? extends LivingEntity> owner() {
        return Optional.ofNullable(handle().getOwner())
                .map(worldRegistry.entityRegistry()::wrap)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast);
    }

    @Override
    public void setOwner(LivingEntity owner) {
        Objects.requireNonNull(owner, "owner");
        runOnServerThread(() -> handle().setOwner((net.minecraft.world.entity.LivingEntity) EntityHandles.unwrap(owner)));
    }
}
