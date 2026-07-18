package io.fand.api.block.custom;

import net.kyori.adventure.key.Key;

/** Handle for a registered custom block. */
public interface CustomBlockRegistration {

    CustomBlockType type();

    default Key id() {
        return type().id();
    }

    boolean active();

    void unregister();

    /** Binds a custom item for placement and default player-break drops. */
    default CustomBlockItemBinding bindItem(Key itemId) {
        throw new UnsupportedOperationException("Custom item bindings are not supported");
    }

    default void unbindItem(Key itemId) {
        throw new UnsupportedOperationException("Custom item bindings are not supported");
    }
}
