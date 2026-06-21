package io.fand.api.entity;

import io.fand.api.world.Location;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public interface Bee extends Animal, Angerable {

    boolean hasHive();

    Optional<Location> hiveLocation();

    void setHiveLocation(@Nullable Location location);

    boolean hasNectar();

    void setHasNectar(boolean hasNectar);

    boolean hasStung();

    void setHasStung(boolean hasStung);

    void setStayOutOfHiveTicks(int ticks);
}
