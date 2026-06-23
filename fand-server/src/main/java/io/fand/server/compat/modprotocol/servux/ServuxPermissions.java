package io.fand.server.compat.modprotocol.servux;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

final class ServuxPermissions {

    private ServuxPermissions() {
    }

    static boolean has(ServerPlayer player, int level) {
        return level <= 0 || player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)));
    }
}
