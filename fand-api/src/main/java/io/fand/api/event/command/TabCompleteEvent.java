package io.fand.api.event.command;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fired on the server thread before command completions are sent to a player.
 *
 * <p>The completion list is mutable. Cancelling the event sends an empty
 * suggestion list for the same replacement range.
 */
public final class TabCompleteEvent implements Event, Cancellable {

    private final Player player;
    private final String buffer;
    private final List<String> completions;
    private boolean cancelled;

    public TabCompleteEvent(Player player, String buffer, List<String> completions) {
        this.player = Objects.requireNonNull(player, "player");
        this.buffer = Objects.requireNonNull(buffer, "buffer");
        this.completions = new ArrayList<>(Objects.requireNonNull(completions, "completions"));
    }

    public Player player() {
        return player;
    }

    /** Raw command buffer from the client, usually including a leading slash. */
    public String buffer() {
        return buffer;
    }

    public List<String> completions() {
        return completions;
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
