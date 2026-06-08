package io.fand.api.event.player;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired after a player-thrown egg impacts and before vanilla spawns chicks.
 */
public final class PlayerEggThrowEvent implements Event {

    private final Player player;
    private final Entity egg;
    private boolean hatching;
    private int hatchCount;

    public PlayerEggThrowEvent(Player player, Entity egg, boolean hatching, int hatchCount) {
        this.player = Objects.requireNonNull(player, "player");
        this.egg = Objects.requireNonNull(egg, "egg");
        this.hatching = hatching;
        this.hatchCount = Math.max(0, hatchCount);
    }

    public Player player() {
        return player;
    }

    public Entity egg() {
        return egg;
    }

    public boolean hatching() {
        return hatching;
    }

    public void setHatching(boolean hatching) {
        this.hatching = hatching;
    }

    public int hatchCount() {
        return hatchCount;
    }

    public void setHatchCount(int hatchCount) {
        this.hatchCount = Math.max(0, hatchCount);
    }
}
