package io.fand.api.tablist;

import io.fand.api.entity.GameMode;
import io.fand.api.player.PlayerProfile;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Per-viewer player-list row. Entries may describe real players, virtual rows,
 * or remote players mirrored from another server.
 */
public final class TabListEntry {

    private final PlayerProfile profile;
    private final boolean listed;
    private final int latency;
    private final GameMode gameMode;
    private final @Nullable Component displayName;
    private final boolean showHat;
    private final int order;

    private TabListEntry(
            PlayerProfile profile,
            boolean listed,
            int latency,
            GameMode gameMode,
            @Nullable Component displayName,
            boolean showHat,
            int order
    ) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.listed = listed;
        this.latency = Math.max(0, latency);
        this.gameMode = Objects.requireNonNull(gameMode, "gameMode");
        this.displayName = displayName;
        this.showHat = showHat;
        this.order = order;
    }

    public static Builder builder(PlayerProfile profile) {
        return new Builder(profile);
    }

    public static Builder builder(UUID uniqueId, String name) {
        return builder(new PlayerProfile(uniqueId, name));
    }

    public PlayerProfile profile() {
        return profile;
    }

    public boolean listed() {
        return listed;
    }

    public int latency() {
        return latency;
    }

    public GameMode gameMode() {
        return gameMode;
    }

    public @Nullable Component displayName() {
        return displayName;
    }

    public boolean showHat() {
        return showHat;
    }

    public int order() {
        return order;
    }

    public TabListEntry withListed(boolean listed) {
        return new TabListEntry(profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public TabListEntry withLatency(int latency) {
        return new TabListEntry(profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public TabListEntry withGameMode(GameMode gameMode) {
        return new TabListEntry(profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public TabListEntry withDisplayName(@Nullable Component displayName) {
        return new TabListEntry(profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public TabListEntry withShowHat(boolean showHat) {
        return new TabListEntry(profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public TabListEntry withOrder(int order) {
        return new TabListEntry(profile, listed, latency, gameMode, displayName, showHat, order);
    }

    public static final class Builder {

        private final PlayerProfile profile;
        private boolean listed = true;
        private int latency;
        private GameMode gameMode = GameMode.SURVIVAL;
        private @Nullable Component displayName;
        private boolean showHat = true;
        private int order;

        private Builder(PlayerProfile profile) {
            this.profile = Objects.requireNonNull(profile, "profile");
        }

        public Builder listed(boolean listed) {
            this.listed = listed;
            return this;
        }

        public Builder latency(int latency) {
            this.latency = latency;
            return this;
        }

        public Builder gameMode(GameMode gameMode) {
            this.gameMode = Objects.requireNonNull(gameMode, "gameMode");
            return this;
        }

        public Builder displayName(@Nullable Component displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder showHat(boolean showHat) {
            this.showHat = showHat;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public TabListEntry build() {
            return new TabListEntry(profile, listed, latency, gameMode, displayName, showHat, order);
        }
    }
}
