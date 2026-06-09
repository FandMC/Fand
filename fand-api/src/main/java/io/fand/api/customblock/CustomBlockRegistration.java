package io.fand.api.customblock;

import net.kyori.adventure.key.Key;

/** Handle for a registered custom block. */
public interface CustomBlockRegistration {

    Key id();

    boolean active();

    void unregister();

    default CustomBlockItemBinding bindItem(Key itemId) {
        throw new UnsupportedOperationException("Custom item bindings are not supported");
    }

    default void unbindItem(Key itemId) {
        throw new UnsupportedOperationException("Custom item bindings are not supported");
    }
}
