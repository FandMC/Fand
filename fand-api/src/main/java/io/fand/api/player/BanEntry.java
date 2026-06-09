package io.fand.api.player;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * User ban-list entry.
 */
public record BanEntry(
        PlayerProfile profile,
        Instant created,
        String source,
        Optional<Instant> expires,
        Optional<String> reason
) {
    public BanEntry {
        profile = Objects.requireNonNull(profile, "profile");
        created = Objects.requireNonNull(created, "created");
        source = Objects.requireNonNull(source, "source");
        expires = Objects.requireNonNull(expires, "expires");
        reason = Objects.requireNonNull(reason, "reason");
    }

    public boolean permanent() {
        return expires.isEmpty();
    }
}
