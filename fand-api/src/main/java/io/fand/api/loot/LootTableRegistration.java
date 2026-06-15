package io.fand.api.loot;

import net.kyori.adventure.key.Key;

public interface LootTableRegistration extends AutoCloseable {

    Key key();

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
