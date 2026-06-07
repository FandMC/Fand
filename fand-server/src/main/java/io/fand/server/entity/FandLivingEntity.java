package io.fand.server.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.server.world.WorldRegistry;

/**
 * Thin handle around a vanilla {@link net.minecraft.world.entity.LivingEntity}
 * for use by API consumers (mostly event dispatch). Reads follow the public
 * entity contract; mutating writes marshal to the server thread via the
 * underlying server.
 *
 * <p>For {@code ServerPlayer} victims prefer the {@link FandPlayer} cached in
 * the registry — its handle is refreshed across respawns and wires up the
 * inventory/permission services.
 */
public final class FandLivingEntity extends FandEntity implements LivingEntity {

    public FandLivingEntity(net.minecraft.world.entity.LivingEntity handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.LivingEntity handle() {
        return (net.minecraft.world.entity.LivingEntity) handle;
    }

    @Override
    public double health() {
        return handle().getHealth();
    }

    @Override
    public double maxHealth() {
        return handle().getMaxHealth();
    }

    @Override
    public void setHealth(double health) {
        var handle = handle();
        var server = handle.level().getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> {
            float clamped = (float) Math.max(0.0, Math.min(health, handle.getMaxHealth()));
            handle.setHealth(clamped);
        };
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
    }

}
