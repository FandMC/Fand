package io.fand.api.entity;

import java.util.Set;
import net.kyori.adventure.key.Key;

/** Snapshot of a player's progress for one advancement. */
public record AdvancementProgress(
        Key advancement,
        boolean done,
        Set<String> completedCriteria,
        Set<String> remainingCriteria
) {

    public AdvancementProgress {
        java.util.Objects.requireNonNull(advancement, "advancement");
        completedCriteria = Set.copyOf(java.util.Objects.requireNonNull(completedCriteria, "completedCriteria"));
        remainingCriteria = Set.copyOf(java.util.Objects.requireNonNull(remainingCriteria, "remainingCriteria"));
    }
}
