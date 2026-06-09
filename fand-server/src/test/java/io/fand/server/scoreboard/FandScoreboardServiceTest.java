package io.fand.server.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;

import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.junit.jupiter.api.Test;

final class FandScoreboardServiceTest {

    @Test
    void staleObjectiveRegistrationDoesNotRemoveReplacement() {
        var scoreboard = new Scoreboard();
        var original = scoreboard.addObjective(
                "kills",
                ObjectiveCriteria.DUMMY,
                net.minecraft.network.chat.Component.literal("Kills"),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null);
        scoreboard.removeObjective(original);
        var replacement = scoreboard.addObjective(
                "kills",
                ObjectiveCriteria.DUMMY,
                net.minecraft.network.chat.Component.literal("Kills 2"),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null);
        var registration = new FandScoreboardRegistration(
                "kills",
                () -> FandScoreboardService.removeObjectiveIfCurrent(scoreboard, "kills", original));

        registration.unregister();

        assertThat(registration.active()).isFalse();
        assertThat(scoreboard.getObjective("kills")).isSameAs(replacement);
    }

    @Test
    void staleTeamRegistrationDoesNotRemoveReplacement() {
        var scoreboard = new Scoreboard();
        var original = scoreboard.addPlayerTeam("red");
        scoreboard.removePlayerTeam(original);
        var replacement = scoreboard.addPlayerTeam("red");
        var registration = new FandScoreboardRegistration(
                "red",
                () -> FandScoreboardService.removeTeamIfCurrent(scoreboard, "red", original));

        registration.unregister();

        assertThat(registration.active()).isFalse();
        assertThat(scoreboard.getPlayerTeam("red")).isSameAs(replacement);
    }
}
