package io.fand.api.scoreboard;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * A score entry for one owner within one objective.
 */
public interface ScoreboardScore {

    String owner();

    ScoreboardObjective objective();

    int value();

    void setValue(int value);

    default void add(int delta) {
        setValue(value() + delta);
    }

    default void subtract(int delta) {
        setValue(value() - delta);
    }

    Optional<Component> displayName();

    void setDisplayName(@Nullable Component displayName);

    boolean locked();

    void setLocked(boolean locked);

    ScoreNumberFormat numberFormat();

    void setNumberFormat(ScoreNumberFormat format);
}
