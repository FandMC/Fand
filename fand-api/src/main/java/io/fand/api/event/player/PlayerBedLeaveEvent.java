package io.fand.api.event.player;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread after a player leaves a bed.
 */
public record PlayerBedLeaveEvent(Player player, Block bed, boolean forcefulWakeUp) implements Event {

    public PlayerBedLeaveEvent(Player player, Block bed, boolean forcefulWakeUp) {
        this.player = Objects.requireNonNull(player, "player");
        this.bed = Objects.requireNonNull(bed, "bed");
        this.forcefulWakeUp = forcefulWakeUp;
    }
}
