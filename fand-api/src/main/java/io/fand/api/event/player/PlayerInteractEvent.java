package io.fand.api.event.player;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread when a player right-clicks. The interaction may be
 * with a block ({@link Action#RIGHT_CLICK_BLOCK}) or with empty space
 * ({@link Action#RIGHT_CLICK_AIR}). Cancelling the event suppresses the
 * default vanilla item or block-use behaviour.
 *
 * <p>Block-break interactions are reported through
 * {@link io.fand.api.event.block.BlockBreakEvent} instead and are not fired
 * here.
 */
public final class PlayerInteractEvent implements Event, Cancellable {

    public enum Action {
        RIGHT_CLICK_BLOCK,
        RIGHT_CLICK_AIR
    }

    public enum Hand {
        MAIN_HAND,
        OFF_HAND
    }

    private final Player player;
    private final Action action;
    private final Hand hand;
    private final Optional<Block> block;
    private boolean cancelled;

    public PlayerInteractEvent(Player player, Action action, Hand hand, Optional<Block> block) {
        this.player = Objects.requireNonNull(player, "player");
        this.action = Objects.requireNonNull(action, "action");
        this.hand = Objects.requireNonNull(hand, "hand");
        this.block = Objects.requireNonNull(block, "block");
    }

    public Player player() {
        return player;
    }

    public Action action() {
        return action;
    }

    public Hand hand() {
        return hand;
    }

    /** Block targeted by the interaction; empty for {@link Action#RIGHT_CLICK_AIR}. */
    public Optional<Block> block() {
        return block;
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
