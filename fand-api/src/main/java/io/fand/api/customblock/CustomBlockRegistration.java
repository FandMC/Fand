package io.fand.api.customblock;

import net.kyori.adventure.key.Key;

/** Handle for a registered custom block. */
public interface CustomBlockRegistration {

    Key id();

    boolean active();

    void unregister();
}
