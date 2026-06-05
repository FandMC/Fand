package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before vanilla applies damage to a player.
 * Cancelling the event aborts the damage application; the player keeps their
 * current health, no knockback is dealt, and damage immunity timers are not
 * advanced.
 *
 * <p>The damage amount is mutable and reflects the post-armor, post-effect
 * value vanilla intends to apply. Setting it to zero is equivalent to
 * cancelling. Negative values are clamped to zero by the runtime.
 *
 * <p>Until a full Entity API exists, this event covers player victims only.
 * Mob-on-mob damage is not currently surfaced.
 */
public final class PlayerDamageEvent implements Event, Cancellable {

    private final Player player;
    private final String cause;
    private double amount;
    private boolean cancelled;

    public PlayerDamageEvent(Player player, String cause, double amount) {
        this.player = Objects.requireNonNull(player, "player");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.amount = amount;
    }

    public Player player() {
        return player;
    }

    /** Vanilla damage-type identifier (e.g. {@code minecraft:fall}, {@code minecraft:player_attack}). */
    public String cause() {
        return cause;
    }

    public double amount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
