package io.fand.api.event.player;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread after a player leaves a bed.
 */
public final class PlayerBedLeaveEvent implements Event {

    private final Player player;
    private final Block bed;
    private final boolean forcefulWakeUp;

    public PlayerBedLeaveEvent(Player player, Block bed, boolean forcefulWakeUp) {
        this.player = Objects.requireNonNull(player, "player");
        this.bed = Objects.requireNonNull(bed, "bed");
        this.forcefulWakeUp = forcefulWakeUp;
    }

    public Player player() {
        return player;
    }

    public Block bed() {
        return bed;
    }

    public boolean forcefulWakeUp() {
        return forcefulWakeUp;
    }
}
