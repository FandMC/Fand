package io.fand.server.world;

import io.fand.api.world.Difficulty;

final class Difficulties {

    private Difficulties() {
    }

    static Difficulty toApi(net.minecraft.world.Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> Difficulty.PEACEFUL;
            case EASY -> Difficulty.EASY;
            case NORMAL -> Difficulty.NORMAL;
            case HARD -> Difficulty.HARD;
        };
    }

    static net.minecraft.world.Difficulty toVanilla(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> net.minecraft.world.Difficulty.PEACEFUL;
            case EASY -> net.minecraft.world.Difficulty.EASY;
            case NORMAL -> net.minecraft.world.Difficulty.NORMAL;
            case HARD -> net.minecraft.world.Difficulty.HARD;
        };
    }
}
