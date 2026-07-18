package io.fand.api.block.custom;

import net.kyori.adventure.key.Key;

/** Handle for a custom item to custom block placement binding. */
public interface CustomBlockItemBinding {

    Key itemId();

    Key blockId();

    boolean active();

    void unregister();
}
