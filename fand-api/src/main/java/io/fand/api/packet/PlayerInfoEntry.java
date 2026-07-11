package io.fand.api.packet;

import io.fand.api.entity.GameMode;
import io.fand.api.player.PlayerProfile;
import io.fand.api.tablist.TabListEntry;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * API-safe player-info packet entry. A profile may be absent from update
 * packets that do not include the {@code ADD_PLAYER} action.
 */
public record PlayerInfoEntry(
        UUID profileId,
        @Nullable PlayerProfile profile,
        boolean listed,
        int latency,
        GameMode gameMode,
        @Nullable Component displayName,
        boolean showHat,
        int order
) {

    public PlayerInfoEntry {
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(gameMode, "gameMode");
        if (profile != null && !profileId.equals(profile.uniqueId())) {
            throw new IllegalArgumentException("profileId must match profile.uniqueId()");
        }
    }

    public static PlayerInfoEntry from(TabListEntry entry) {
        Objects.requireNonNull(entry, "entry");
        return new PlayerInfoEntry(
                entry.profile().uniqueId(),
                entry.profile(),
                entry.listed(),
                entry.latency(),
                entry.gameMode(),
                entry.displayName(),
                entry.showHat(),
                entry.order());
    }

    public PlayerInfoEntry withProfile(@Nullable PlayerProfile profile) {
        return new PlayerInfoEntry(profileId, profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public PlayerInfoEntry withListed(boolean listed) {
        return new PlayerInfoEntry(profileId, profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public PlayerInfoEntry withLatency(int latency) {
        return new PlayerInfoEntry(profileId, profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public PlayerInfoEntry withGameMode(GameMode gameMode) {
        return new PlayerInfoEntry(profileId, profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public PlayerInfoEntry withDisplayName(@Nullable Component displayName) {
        return new PlayerInfoEntry(profileId, profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public PlayerInfoEntry withShowHat(boolean showHat) {
        return new PlayerInfoEntry(profileId, profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public PlayerInfoEntry withOrder(int order) {
        return new PlayerInfoEntry(profileId, profile, listed, latency, gameMode, displayName, showHat, order);
    }
}
