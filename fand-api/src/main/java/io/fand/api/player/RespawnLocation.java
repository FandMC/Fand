package io.fand.api.player;

import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Personal player respawn location.
 */
public record RespawnLocation(Location location, boolean forced) {

    public RespawnLocation {
        location = Objects.requireNonNull(location, "location");
    }
}
