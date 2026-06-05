package io.fand.server.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.server.world.WorldRegistry;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minecraft.world.entity.EntityType;

/**
 * Thin handle around a vanilla {@link net.minecraft.world.entity.LivingEntity}
 * for use by API consumers (mostly event dispatch). Reads run on the caller's
 * thread; mutating writes thunk to the main thread via the underlying server.
 *
 * <p>For {@code ServerPlayer} victims prefer the {@link FandPlayer} cached in
 * the registry — its handle is refreshed across respawns and wires up the
 * inventory/permission services.
 */
public final class FandLivingEntity implements LivingEntity {

    private final net.minecraft.world.entity.LivingEntity handle;
    private final WorldRegistry worldRegistry;

    public FandLivingEntity(net.minecraft.world.entity.LivingEntity handle, WorldRegistry worldRegistry) {
        this.handle = Objects.requireNonNull(handle, "handle");
        this.worldRegistry = Objects.requireNonNull(worldRegistry, "worldRegistry");
    }

    public net.minecraft.world.entity.LivingEntity handle() {
        return handle;
    }

    @Override
    public UUID uniqueId() {
        return handle.getUUID();
    }

    @Override
    public int entityId() {
        return handle.getId();
    }

    @Override
    public Key type() {
        var identifier = EntityType.getKey(handle.getType());
        return Key.key(identifier.getNamespace(), identifier.getPath());
    }

    @Override
    public boolean alive() {
        return handle.isAlive();
    }

    @Override
    public Location location() {
        return new Location(world(), handle.getX(), handle.getY(), handle.getZ(), handle.getYRot(), handle.getXRot());
    }

    @Override
    public World world() {
        if (handle.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return worldRegistry.wrap(serverLevel);
        }
        throw new IllegalStateException("Living entity is not in a server level: " + handle);
    }

    @Override
    public double health() {
        return handle.getHealth();
    }

    @Override
    public double maxHealth() {
        return handle.getMaxHealth();
    }

    @Override
    public void setHealth(double health) {
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

    @Override
    public boolean equals(Object other) {
        return other instanceof io.fand.api.entity.Entity that && this.uniqueId().equals(that.uniqueId());
    }

    @Override
    public int hashCode() {
        return uniqueId().hashCode();
    }
}
