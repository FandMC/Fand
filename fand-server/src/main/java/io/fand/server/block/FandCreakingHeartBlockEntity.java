package io.fand.server.block;

import io.fand.api.block.CreakingHeartBlockEntity;
import io.fand.api.entity.LivingEntity;
import io.fand.server.hooks.FandHooks;
import java.util.Optional;

public final class FandCreakingHeartBlockEntity extends FandBlockEntity implements CreakingHeartBlockEntity {
    public FandCreakingHeartBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.CreakingHeartBlockEntity handle) { super(block, handle); }
    @Override public net.minecraft.world.level.block.entity.CreakingHeartBlockEntity handle() { return (net.minecraft.world.level.block.entity.CreakingHeartBlockEntity) super.handle(); }

    @Override
    public Optional<? extends LivingEntity> protector() {
        return block.callOnServerThread(() -> handle().fand$getProtector()
                .map(FandHooks::wrapEntity)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast));
    }

    @Override public void removeProtector() { block.runOnServerThread(() -> handle().removeProtector(null)); }
    @Override public int analogOutputSignal() { return block.callOnServerThread(() -> handle().getAnalogOutputSignal()); }
}
