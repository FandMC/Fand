package io.fand.api.player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Stable player identity used by access lists, offline lookups, and profile
 * texture data.
 */
public final class PlayerProfile {

    private final UUID uniqueId;
    private final String name;
    private final @Nullable PlayerSkin skin;

    public PlayerProfile(UUID uniqueId, String name) {
        this(uniqueId, name, null);
    }

    public PlayerProfile(UUID uniqueId, String name, @Nullable PlayerSkin skin) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
        this.name = Objects.requireNonNull(name, "name").trim();
        this.skin = skin;
        if (this.name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }

    public UUID uniqueId() {
        return uniqueId;
    }

    public String name() {
        return name;
    }

    public Optional<PlayerSkin> skin() {
        return Optional.ofNullable(skin);
    }

    public @Nullable PlayerSkin skinOrNull() {
        return skin;
    }

    public PlayerProfile withSkin(@Nullable PlayerSkin skin) {
        return new PlayerProfile(uniqueId, name, skin);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof PlayerProfile that
                && uniqueId.equals(that.uniqueId)
                && name.equals(that.name)
                && Objects.equals(skin, that.skin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, name, skin);
    }

    @Override
    public String toString() {
        return "PlayerProfile[uniqueId=" + uniqueId + ", name=" + name + ", skin=" + (skin != null) + "]";
    }
}
