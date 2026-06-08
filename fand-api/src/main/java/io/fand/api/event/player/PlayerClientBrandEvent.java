package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread when a player reports their client brand.
 */
public final class PlayerClientBrandEvent implements Event {

    private final Player player;
    private final String brand;

    public PlayerClientBrandEvent(Player player, String brand) {
        this.player = Objects.requireNonNull(player, "player");
        this.brand = Objects.requireNonNull(brand, "brand");
    }

    public Player player() {
        return player;
    }

    public String brand() {
        return brand;
    }
}
