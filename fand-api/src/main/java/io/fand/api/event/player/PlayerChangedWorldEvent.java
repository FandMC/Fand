package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.world.World;
import java.util.Objects;

/**
 * Fired on the server thread after a player successfully changes worlds.
 */
public record PlayerChangedWorldEvent(Player player, World fromWorld, World toWorld) implements Event {

    public PlayerChangedWorldEvent(Player player, World fromWorld, World toWorld) {
        this.player = Objects.requireNonNull(player, "player");
        this.fromWorld = Objects.requireNonNull(fromWorld, "fromWorld");
        this.toWorld = Objects.requireNonNull(toWorld, "toWorld");
    }
}
