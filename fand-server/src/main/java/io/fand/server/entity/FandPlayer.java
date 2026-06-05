package io.fand.server.entity;

import io.fand.api.entity.Player;
import io.fand.api.permission.PermissionService;
import io.fand.server.command.AdventureBridge;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;

public final class FandPlayer implements Player {

    private final ServerPlayer handle;
    private final PermissionService permissions;

    public FandPlayer(ServerPlayer handle, PermissionService permissions) {
        this.handle = handle;
        this.permissions = permissions;
    }

    public ServerPlayer handle() {
        return handle;
    }

    @Override
    public UUID uniqueId() {
        return handle.getUUID();
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
