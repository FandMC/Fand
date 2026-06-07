package io.fand.api;

import net.kyori.adventure.key.Key;

/** Common contract for generated vanilla registry key enums. */
public interface VanillaKey {

    Key key();

    default String asString() {
        return key().asString();
    }
}
