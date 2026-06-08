package io.fand.api.entity;

import net.kyori.adventure.key.Key;

/**
 * Vanilla entity type exposed without leaking server implementation classes.
 */
public interface EntityType {

    Key key();

    boolean spawnable();

    boolean player();
}
