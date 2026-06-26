package io.fand.api.region;

import net.kyori.adventure.key.Key;

public interface RegionRegistration extends AutoCloseable {

    Key key();

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
