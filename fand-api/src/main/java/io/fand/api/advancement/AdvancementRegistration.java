package io.fand.api.advancement;

import net.kyori.adventure.key.Key;

public interface AdvancementRegistration extends AutoCloseable {

    Key key();

    boolean active();

    @Override
    void close();
}
