package io.fand.api.player;

import java.util.Objects;

/**
 * Operator-list entry.
 */
public record OperatorEntry(PlayerProfile profile, OperatorLevel level, boolean bypassesPlayerLimit) {

    public OperatorEntry {
        profile = Objects.requireNonNull(profile, "profile");
        level = Objects.requireNonNull(level, "level");
    }
}
