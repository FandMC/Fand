package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a player's food level or saturation changes.
 */
public final class PlayerFoodLevelChangeEvent implements Event, Cancellable {

    private final Player player;
    private final int fromLevel;
    private int toLevel;
    private final float fromSaturation;
    private float toSaturation;
    private final Cause cause;
    private boolean cancelled;

    public PlayerFoodLevelChangeEvent(
            Player player,
            int fromLevel,
            int toLevel,
            float fromSaturation,
            float toSaturation,
            Cause cause
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.fromLevel = clampFood(fromLevel);
        this.toLevel = clampFood(toLevel);
        this.fromSaturation = Math.max(0.0F, fromSaturation);
        this.toSaturation = Math.max(0.0F, toSaturation);
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public Player player() {
        return player;
    }

    public int fromLevel() {
        return fromLevel;
    }

    public int toLevel() {
        return toLevel;
    }

    public void setToLevel(int toLevel) {
        this.toLevel = clampFood(toLevel);
    }

    public float fromSaturation() {
        return fromSaturation;
    }

    public float toSaturation() {
        return toSaturation;
    }

    public void setToSaturation(float toSaturation) {
        this.toSaturation = Math.max(0.0F, toSaturation);
    }

    public Cause cause() {
        return cause;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    private static int clampFood(int level) {
        return Math.max(0, Math.min(20, level));
    }

    public enum Cause {
        EAT,
        EXHAUSTION,
        PEACEFUL_REGEN,
        PLUGIN,
        UNKNOWN
    }
}
