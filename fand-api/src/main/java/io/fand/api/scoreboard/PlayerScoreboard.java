package io.fand.api.scoreboard;

import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;

/**
 * Per-player scoreboard display overrides.
 *
 * <p>This does not create a separate persistent vanilla scoreboard. It controls
 * which existing objectives are shown to one player, allowing different players
 * to see different sidebar/list/below-name objectives.
 */
public interface PlayerScoreboard {

    Map<String, ? extends ScoreboardObjective> objectives();

    Optional<? extends ScoreboardObjective> objective(String name);

    ScoreboardRegistration registerObjective(String name, Component displayName);

    ScoreboardRegistration registerObjective(String name, Component displayName, ScoreRenderType renderType);

    boolean removeObjective(String name);

    Map<String, ? extends ScoreboardTeam> teams();

    Optional<? extends ScoreboardTeam> team(String name);

    ScoreboardRegistration registerTeam(String name);

    boolean removeTeam(String name);

    Optional<? extends ScoreboardTeam> teamOf(String member);

    boolean removeMemberFromTeam(String member);

    Optional<? extends ScoreboardObjective> displayedObjective(ScoreDisplaySlot slot);

    Map<ScoreDisplaySlot, ? extends ScoreboardObjective> displayedObjectives();

    void setDisplayedObjective(ScoreDisplaySlot slot, ScoreboardObjective objective);

    void clearDisplayedObjective(ScoreDisplaySlot slot);

    void clearDisplayedObjectives();

    void resetDisplayedObjective(ScoreDisplaySlot slot);

    void resetDisplayedObjectives();
}
