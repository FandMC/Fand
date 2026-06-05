package io.fand.server.entity;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.entity.GameMode;
import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.Test;

class GameModesTest {

    @Test
    void roundTripsEveryApiValue() {
        for (var mode : GameMode.values()) {
            assertThat(GameModes.toApi(GameModes.toVanilla(mode)))
                    .as("round-trip for %s", mode)
                    .isEqualTo(mode);
        }
    }

    @Test
    void roundTripsEveryVanillaValue() {
        for (var type : GameType.values()) {
            assertThat(GameModes.toVanilla(GameModes.toApi(type)))
                    .as("round-trip for %s", type)
                    .isEqualTo(type);
        }
    }
}
