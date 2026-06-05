package io.fand.server.entity;

import io.fand.api.entity.GameMode;
import net.minecraft.world.level.GameType;

/**
 * Two-way mapping between the public {@link GameMode} enum and the vanilla
 * {@link GameType} enum.
 *
 * <p>Every enum value on each side maps; switch expressions stay exhaustive so
 * adding a new mode is a compile error here first.
 */
final class GameModes {

    private GameModes() {
    }

    static GameMode toApi(GameType type) {
        return switch (type) {
            case SURVIVAL -> GameMode.SURVIVAL;
            case CREATIVE -> GameMode.CREATIVE;
            case ADVENTURE -> GameMode.ADVENTURE;
            case SPECTATOR -> GameMode.SPECTATOR;
        };
    }

    static GameType toVanilla(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> GameType.SURVIVAL;
            case CREATIVE -> GameType.CREATIVE;
            case ADVENTURE -> GameType.ADVENTURE;
            case SPECTATOR -> GameType.SPECTATOR;
        };
    }
}
