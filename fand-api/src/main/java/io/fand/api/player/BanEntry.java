package io.fand.api.player;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * User ban-list entry.
 */
public final class BanEntry {

    private final PlayerProfile profile;
    private final Instant created;
    private final String source;
    private final @Nullable Instant expires;
    private final @Nullable String reason;

    public BanEntry(
            PlayerProfile profile,
            Instant created,
            String source,
            @Nullable Instant expires,
            @Nullable String reason) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.created = Objects.requireNonNull(created, "created");
        this.source = Objects.requireNonNull(source, "source");
        this.expires = expires;
        this.reason = reason;
    }

    public PlayerProfile profile() {
        return profile;
    }

    public Instant created() {
        return created;
    }

    public String source() {
        return source;
    }

    public Optional<Instant> expires() {
        return Optional.ofNullable(expires);
    }

    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    public boolean permanent() {
        return expires == null;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof BanEntry that
                && Objects.equals(profile, that.profile)
                && Objects.equals(created, that.created)
                && Objects.equals(source, that.source)
                && Objects.equals(expires, that.expires)
                && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profile, created, source, expires, reason);
    }

    @Override
    public String toString() {
        return "BanEntry[profile=" + profile + ", created=" + created + ", source=" + source
                + ", expires=" + expires + ", reason=" + reason + "]";
    }
}
