package io.fand.server.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class ScoreboardNamesTest {

    @Test
    void normalizesGlobalNames() {
        assertThat(ScoreboardNames.normalizeObjective(" Test.Name ")).isEqualTo("test.name");
        assertThat(ScoreboardNames.normalizeTeam("Team-1")).isEqualTo("team-1");
    }

    @Test
    void scopesPluginNamesWithinVanillaLengthLimit() {
        assertThat(ScoreboardNames.normalizePluginObjective("plug", "kills")).isEqualTo("plug:kills");
        assertThat(ScoreboardNames.normalizePluginTeam("plug", "minecraft:blue")).isEqualTo("plug:blue");
    }

    @Test
    void rejectsInvalidOrTooLongNames() {
        assertThatThrownBy(() -> ScoreboardNames.normalizeObjective("Bad Name"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ScoreboardNames.normalizePluginObjective("very-long-plugin", "objective"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
