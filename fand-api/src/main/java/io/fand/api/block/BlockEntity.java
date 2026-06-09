package io.fand.api.block;

import net.kyori.adventure.key.Key;

/**
 * Block entity attached to a live block position.
 */
public interface BlockEntity {

    Block block();

    Key type();

    boolean removed();
}
