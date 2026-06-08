package io.fand.api.event.player;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread when a player's fishing hook changes meaningful
 * state or is reeled in.
 */
public final class PlayerFishEvent implements Event, Cancellable {

    private final Player player;
    private final Entity hook;
    private final State state;
    private final Optional<Entity> caught;
    private final List<ItemStack> drops;
    private boolean cancelled;

    public PlayerFishEvent(Player player, Entity hook, State state, Optional<Entity> caught, List<ItemStack> drops) {
        this.player = Objects.requireNonNull(player, "player");
        this.hook = Objects.requireNonNull(hook, "hook");
        this.state = Objects.requireNonNull(state, "state");
        this.caught = Objects.requireNonNull(caught, "caught");
        this.drops = List.copyOf(Objects.requireNonNull(drops, "drops"));
    }

    public Player player() {
        return player;
    }

    public Entity hook() {
        return hook;
    }

    public State state() {
        return state;
    }

    public Optional<Entity> caught() {
        return caught;
    }

    public List<ItemStack> drops() {
        return drops;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public enum State {
        FISHING,
        BITE,
        CAUGHT_FISH,
        CAUGHT_ENTITY,
        IN_GROUND,
        REEL_IN,
        FAILED
    }
}
