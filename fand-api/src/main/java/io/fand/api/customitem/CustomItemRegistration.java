package io.fand.api.customitem;

import net.kyori.adventure.key.Key;

/** Handle for a registered custom item. */
public interface CustomItemRegistration {

    Key id();

    boolean active();

    void unregister();
}
