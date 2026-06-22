package io.fand.api.player;

import io.fand.api.entity.GameMode;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Initial options for a server-side simulated player.
 */
public final class SimulatedPlayerOptions {

    private final @Nullable GameMode gameMode;
    private final boolean broadcastJoinMessage;
    private final boolean broadcastQuitMessage;
    private final boolean saveData;

    private SimulatedPlayerOptions(
            @Nullable GameMode gameMode,
            boolean broadcastJoinMessage,
            boolean broadcastQuitMessage,
            boolean saveData
    ) {
        this.gameMode = gameMode;
        this.broadcastJoinMessage = broadcastJoinMessage;
        this.broadcastQuitMessage = broadcastQuitMessage;
        this.saveData = saveData;
    }

    public static SimulatedPlayerOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<GameMode> gameMode() {
        return Optional.ofNullable(gameMode);
    }

    public @Nullable GameMode gameModeOrNull() {
        return gameMode;
    }

    public boolean broadcastJoinMessage() {
        return broadcastJoinMessage;
    }

    public boolean broadcastQuitMessage() {
        return broadcastQuitMessage;
    }

    public boolean saveData() {
        return saveData;
    }

    public static final class Builder {

        private @Nullable GameMode gameMode;
        private boolean broadcastJoinMessage;
        private boolean broadcastQuitMessage;
        private boolean saveData = true;

        private Builder() {
        }

        public Builder gameMode(GameMode gameMode) {
            this.gameMode = Objects.requireNonNull(gameMode, "gameMode");
            return this;
        }

        public Builder keepSavedGameMode() {
            this.gameMode = null;
            return this;
        }

        public Builder broadcastJoinMessage(boolean broadcastJoinMessage) {
            this.broadcastJoinMessage = broadcastJoinMessage;
            return this;
        }

        public Builder broadcastQuitMessage(boolean broadcastQuitMessage) {
            this.broadcastQuitMessage = broadcastQuitMessage;
            return this;
        }

        public Builder saveData(boolean saveData) {
            this.saveData = saveData;
            return this;
        }

        public SimulatedPlayerOptions build() {
            return new SimulatedPlayerOptions(gameMode, broadcastJoinMessage, broadcastQuitMessage, saveData);
        }
    }
}
