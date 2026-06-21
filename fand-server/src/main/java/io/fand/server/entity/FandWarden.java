package io.fand.server.entity;

import io.fand.api.entity.Entity;
import io.fand.api.entity.LivingEntity;
import io.fand.api.entity.Warden;
import io.fand.server.world.WorldRegistry;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class FandWarden extends FandMob implements Warden {

    public FandWarden(net.minecraft.world.entity.monster.warden.Warden handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.monster.warden.Warden handle() {
        return (net.minecraft.world.entity.monster.warden.Warden) handle;
    }

    @Override
    public AngerLevel angerLevel() {
        return AngerLevel.valueOf(handle().getAngerLevel().name());
    }

    @Override
    public int anger() {
        return handle().getAngerManagement().getActiveAnger(handle().getTarget());
    }

    @Override
    public Optional<? extends LivingEntity> activeAngerTarget() {
        return handle().getEntityAngryAt()
                .map(worldRegistry.entityRegistry()::wrap)
                .map(LivingEntity.class::cast);
    }

    @Override
    public void increaseAnger(@Nullable Entity entity, int amount, boolean playSound) {
        var target = entity == null ? null : EntityHandles.unwrap(entity);
        runOnServerThread(() -> handle().increaseAngerAt(target, Math.max(0, amount), playSound));
    }

    @Override
    public void clearAnger(Entity entity) {
        java.util.Objects.requireNonNull(entity, "entity");
        runOnServerThread(() -> handle().clearAnger(EntityHandles.unwrap(entity)));
    }
}
