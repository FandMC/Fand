package io.fand.api.scoreboard;

import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.text.Component;

/**
 * A persistent vanilla scoreboard objective.
 */
public interface ScoreboardObjective {

    String name();

    String criteria();

    boolean readOnly();

    Component displayName();

    void setDisplayName(Component displayName);

    ScoreRenderType renderType();

    void setRenderType(ScoreRenderType renderType);

    boolean displayAutoUpdate();

    void setDisplayAutoUpdate(boolean displayAutoUpdate);

    ScoreNumberFormat numberFormat();

    void setNumberFormat(ScoreNumberFormat format);

    ScoreboardScore score(String owner);

    Optional<? extends ScoreboardScore> existingScore(String owner);

    /** Point-in-time snapshot of this objective's scores; immutable. */
    Collection<? extends ScoreboardScore> scores();

    boolean resetScore(String owner);

    void unregister();
}
