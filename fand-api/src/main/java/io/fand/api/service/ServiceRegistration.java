package io.fand.api.service;

import net.kyori.adventure.key.Key;

public interface ServiceRegistration<T> extends AutoCloseable {

    Key key();

    Class<T> type();

    T service();

    String owner();

    ServicePriority priority();

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
