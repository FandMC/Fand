package io.fand.server.entity;

import io.fand.api.entity.Explosive;
import io.fand.api.entity.LivingEntity;
import io.fand.server.world.WorldRegistry;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class FandExplosive extends FandEntity implements Explosive {

    public FandExplosive(net.minecraft.world.entity.item.PrimedTnt handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.item.PrimedTnt handle() {
        return (net.minecraft.world.entity.item.PrimedTnt) handle;
    }

    @Override
    public int fuseTicks() {
        return handle().getFuse();
    }

    @Override
    public void setFuseTicks(int ticks) {
        runOnServerThread(() -> handle().setFuse(Math.max(0, ticks)));
    }

    @Override
    public Optional<? extends LivingEntity> owner() {
        return Optional.ofNullable(handle().getOwner())
                .map(worldRegistry.entityRegistry()::wrap)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast);
    }

    @Override
    public void setOwner(@Nullable LivingEntity owner) {
        runOnServerThread(() -> handle().fand$setOwner(owner == null
                ? null
                : (net.minecraft.world.entity.LivingEntity) EntityHandles.unwrap(owner)));
    }
}
