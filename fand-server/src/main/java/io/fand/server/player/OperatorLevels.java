package io.fand.server.player;

import io.fand.api.player.OperatorLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;

final class OperatorLevels {

    private OperatorLevels() {
    }

    static OperatorLevel fromVanilla(PermissionLevel level) {
        return switch (level) {
            case ALL -> OperatorLevel.ALL;
            case MODERATORS -> OperatorLevel.MODERATORS;
            case GAMEMASTERS -> OperatorLevel.GAMEMASTERS;
            case ADMINS -> OperatorLevel.ADMINS;
            case OWNERS -> OperatorLevel.OWNERS;
        };
    }

    static LevelBasedPermissionSet toVanilla(OperatorLevel level) {
        return LevelBasedPermissionSet.forLevel(switch (level) {
            case ALL -> PermissionLevel.ALL;
            case MODERATORS -> PermissionLevel.MODERATORS;
            case GAMEMASTERS -> PermissionLevel.GAMEMASTERS;
            case ADMINS -> PermissionLevel.ADMINS;
            case OWNERS -> PermissionLevel.OWNERS;
        });
    }
}
