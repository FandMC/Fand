package io.fand.api.structure;

import net.kyori.adventure.key.Key;

/** Handle returned by custom structure registration. */
public interface StructureRegistration extends AutoCloseable {

    Key key();

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
