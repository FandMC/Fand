package io.fand.api.scoreboard;

import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.text.Component;

/**
 * Persistent vanilla scoreboard service.
 */
public interface ScoreboardService {

    /** Point-in-time snapshot of the server's objectives; immutable. */
    Collection<? extends ScoreboardObjective> objectives();

    Optional<? extends ScoreboardObjective> objective(String name);

    ScoreboardRegistration registerObjective(String name, Component displayName);

    ScoreboardRegistration registerObjective(String name, Component displayName, String criteria, ScoreRenderType renderType);

    boolean removeObjective(String name);

    Optional<? extends ScoreboardObjective> displayedObjective(ScoreDisplaySlot slot);

    void setDisplayedObjective(ScoreDisplaySlot slot, ScoreboardObjective objective);

    void clearDisplayedObjective(ScoreDisplaySlot slot);

    /** Point-in-time snapshot of the server's teams; immutable. */
    Collection<? extends ScoreboardTeam> teams();

    Optional<? extends ScoreboardTeam> team(String name);

    ScoreboardRegistration registerTeam(String name);

    boolean removeTeam(String name);

    Optional<? extends ScoreboardTeam> teamOf(String member);

    boolean removeMemberFromTeam(String member);
}
