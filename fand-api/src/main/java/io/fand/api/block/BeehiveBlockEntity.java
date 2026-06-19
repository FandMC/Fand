package io.fand.api.block;

import io.fand.api.world.Location;
import java.util.Optional;

public interface BeehiveBlockEntity extends BlockEntity {
    int beeCount();

    boolean empty();

    boolean full();

    boolean sedated();

    void releaseBees();

    void releaseBees(BeeReleaseMode mode);

    Optional<Location> flowerPosition();

    void setFlowerPosition(Location location);

    void clearFlowerPosition();
}
