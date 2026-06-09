package io.fand.api.player;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable player identity used by access lists and offline lookups.
 */
public record PlayerProfile(UUID uniqueId, String name) {

    public PlayerProfile {
        uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
        name = Objects.requireNonNull(name, "name").trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }
}
