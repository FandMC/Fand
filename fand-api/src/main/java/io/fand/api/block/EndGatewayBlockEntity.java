package io.fand.api.block;

import io.fand.api.world.Location;
import java.util.Optional;

/** End gateway block entity. */
public interface EndGatewayBlockEntity extends BlockEntity {

    boolean spawning();

    boolean coolingDown();

    float spawnPercent();

    float cooldownPercent();

    Optional<Location> exitPosition();

    void setExitPosition(Location position, boolean exact);

    void triggerCooldown();

    int particleAmount();
}
