package io.fand.api.event.player;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a player enters a bed.
 */
public final class PlayerBedEnterEvent implements Event, Cancellable {

    private final Player player;
    private final Block bed;
    private boolean cancelled;

    public PlayerBedEnterEvent(Player player, Block bed) {
        this.player = Objects.requireNonNull(player, "player");
        this.bed = Objects.requireNonNull(bed, "bed");
    }

    public Player player() {
        return player;
    }

    public Block bed() {
        return bed;
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
