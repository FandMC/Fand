package io.fand.api.item.custom;

import net.kyori.adventure.key.Key;

/** Handle for a registered custom item. */
public interface CustomItemRegistration {

    CustomItemType type();

    default Key id() {
        return type().id();
    }

    boolean active();

    void unregister();
}
