package io.fand.server.entity;

import io.fand.api.entity.Player;
import io.fand.api.permission.PermissionService;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.server.command.AdventureBridge;
import io.fand.server.inventory.FandPlayerInventory;
import io.fand.server.world.FandWorld;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

public final class FandPlayer implements Player {

    private volatile ServerPlayer handle;
    private final PermissionService permissions;
    private final PlayerRegistry registry;
    private volatile FandPlayerInventory inventory;

    public FandPlayer(ServerPlayer handle, PermissionService permissions, PlayerRegistry registry) {
        this.handle = handle;
        this.permissions = permissions;
        this.registry = registry;
        this.inventory = new FandPlayerInventory(handle.getInventory());
    }

    public ServerPlayer handle() {
        return handle;
    }

    void refreshHandle(ServerPlayer newHandle) {
        this.handle = newHandle;
        this.inventory = new FandPlayerInventory(newHandle.getInventory());
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
        return online() && handle.isAlive();
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
    public boolean online() {
        return !handle.hasDisconnected();
    }

    @Override
    public void kick(Component reason) {
        if (handle.connection != null) {
            handle.connection.disconnect(AdventureBridge.toVanilla(reason, handle.registryAccess()));
        }
    }

    @Override
    public Location location() {
        var world = registry.wrapLevel(handle.level());
        return new Location(world, handle.getX(), handle.getY(), handle.getZ(), handle.getYRot(), handle.getXRot());
    }

    @Override
    public World world() {
        return registry.wrapLevel(handle.level());
    }

    @Override
    public CompletableFuture<Boolean> teleport(Location destination) {
        var server = handle.level().getServer();
        if (server == null) {
            return CompletableFuture.completedFuture(false);
        }
        ServerLevel target;
        try {
            target = resolveLevel(destination.world(), server);
        } catch (IllegalArgumentException failure) {
            return CompletableFuture.failedFuture(failure);
        }
        var future = new CompletableFuture<Boolean>();
        Runnable run = () -> {
            if (!online()) {
                future.complete(false);
                return;
            }
            try {
                var ok = handle.teleportTo(
                        target,
                        destination.x(),
                        destination.y(),
                        destination.z(),
                        Set.of(),
                        destination.yaw(),
                        destination.pitch(),
                        true
                );
                future.complete(ok);
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        };
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
        return future;
    }

    private static ServerLevel resolveLevel(World world, net.minecraft.server.MinecraftServer server) {
        if (world instanceof FandWorld fand) {
            return fand.handle();
        }
        var key = world.key();
        for (var level : server.getAllLevels()) {
            var identifier = level.dimension().identifier();
            if (identifier.getNamespace().equals(key.namespace()) && identifier.getPath().equals(key.value())) {
                return level;
            }
        }
        throw new IllegalArgumentException("World not loaded: " + key.asString());
    }

    @Override
    public String name() {
        return handle.getGameProfile().name();
    }

    @Override
    public void sendMessage(Component message) {
        handle.sendSystemMessage(AdventureBridge.toVanilla(message, handle.registryAccess()));
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions.hasPermission(this, permission);
    }

    @Override
    public boolean operator() {
        var server = handle.level().getServer();
        return server != null && server.getPlayerList().isOp(handle.nameAndId());
    }

    @Override
    public Optional<Boolean> permissionValue(String node) {
        return Optional.empty();
    }

    @Override
    public io.fand.api.inventory.PlayerInventory inventory() {
        return inventory;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandPlayer that && this.uniqueId().equals(that.uniqueId());
    }

    @Override
    public int hashCode() {
        return uniqueId().hashCode();
    }

    @Override
    public String toString() {
        return "FandPlayer(" + name() + ")";
    }
}
