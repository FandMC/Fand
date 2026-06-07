package io.fand.server.entity;

import io.fand.api.entity.Entity;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.server.world.WorldRegistry;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minecraft.world.entity.EntityType;

/**
 * Thin handle around a vanilla entity without assuming living-entity features.
 */
public final class FandEntity implements Entity {

    private final net.minecraft.world.entity.Entity handle;
    private final WorldRegistry worldRegistry;

    public FandEntity(net.minecraft.world.entity.Entity handle, WorldRegistry worldRegistry) {
        this.handle = Objects.requireNonNull(handle, "handle");
        this.worldRegistry = Objects.requireNonNull(worldRegistry, "worldRegistry");
    }

    public net.minecraft.world.entity.Entity handle() {
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
        throw new IllegalStateException("Entity is not in a server level: " + handle);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Entity that && this.uniqueId().equals(that.uniqueId());
    }

    @Override
    public int hashCode() {
        return uniqueId().hashCode();
    }
}
