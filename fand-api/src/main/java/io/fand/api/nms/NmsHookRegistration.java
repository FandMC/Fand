package io.fand.api.nms;

import io.fand.api.service.ServicePriority;
import net.kyori.adventure.key.Key;

public interface NmsHookRegistration extends AutoCloseable {

    Key hook();

    NmsHook hookHandler();

    String owner();

    ServicePriority priority();

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
