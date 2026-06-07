package io.fand.server.entity;

import io.fand.api.component.DataComponentContainer;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.server.component.EntityComponentStorage;
import io.fand.server.world.WorldRegistry;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minecraft.world.entity.EntityType;

/**
 * Thin handle around any vanilla entity for API consumers.
 */
public class FandEntity implements io.fand.api.entity.Entity {

    protected final net.minecraft.world.entity.Entity handle;
    protected final WorldRegistry worldRegistry;

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
    public DataComponentContainer components() {
        var server = handle.level().getServer();
        if (server == null) {
            throw new IllegalStateException("Entity is not attached to a server: " + handle);
        }
        if (!server.isSameThread()) {
            throw new IllegalStateException("Entity.components() must be accessed on the server thread");
        }
        return EntityComponentStorage.container(server, handle.getUUID());
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
