package io.fand.api.gamerule;

import net.kyori.adventure.key.Key;

/** Handle returned by custom game rule registration. */
public interface GameRuleRegistration extends AutoCloseable {

    Key key();

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
