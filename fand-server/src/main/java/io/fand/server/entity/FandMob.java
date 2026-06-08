package io.fand.server.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.Mob;
import io.fand.server.world.WorldRegistry;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class FandMob extends FandLivingEntity implements Mob {

    public FandMob(net.minecraft.world.entity.Mob handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.Mob handle() {
        return (net.minecraft.world.entity.Mob) handle;
    }

    @Override
    public Optional<? extends LivingEntity> target() {
        var target = handle().getTarget();
        if (target == null) {
            return Optional.empty();
        }
        return Optional.of(worldRegistry.entityRegistry().wrap(target));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        runOnServerThread(() -> handle().setTarget(target == null
                ? null
                : (net.minecraft.world.entity.LivingEntity) EntityHandles.unwrap(target)));
    }

    @Override
    public boolean noAi() {
        return handle().isNoAi();
    }

    @Override
    public void setNoAi(boolean noAi) {
        runOnServerThread(() -> handle().setNoAi(noAi));
    }

    @Override
    public boolean aggressive() {
        return handle().isAggressive();
    }

    @Override
    public void setAggressive(boolean aggressive) {
        runOnServerThread(() -> handle().setAggressive(aggressive));
    }

    @Override
    public boolean persistent() {
        return handle().isPersistenceRequired();
    }

    @Override
    public void setPersistent() {
        runOnServerThread(() -> handle().setPersistenceRequired());
    }

    @Override
    public boolean canPickUpLoot() {
        return handle().canPickUpLoot();
    }

    @Override
    public void setCanPickUpLoot(boolean canPickUpLoot) {
        runOnServerThread(() -> handle().setCanPickUpLoot(canPickUpLoot));
    }

    @Override
    public boolean leftHanded() {
        return handle().isLeftHanded();
    }

    @Override
    public void setLeftHanded(boolean leftHanded) {
        runOnServerThread(() -> handle().setLeftHanded(leftHanded));
    }
}
