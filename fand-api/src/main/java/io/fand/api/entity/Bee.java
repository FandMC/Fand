package io.fand.api.entity;

import io.fand.api.world.Location;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public interface Bee extends Animal, Angerable {

    boolean hivePresent();

    Optional<Location> hiveLocation();

    void setHiveLocation(@Nullable Location location);

    boolean nectar();

    void setNectar(boolean nectar);

    boolean stung();

    void setStung(boolean stung);

    void setStayOutOfHiveTicks(int ticks);
}
