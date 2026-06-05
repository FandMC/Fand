package io.fand.api.block;

import net.kyori.adventure.key.Key;

/**
 * A Minecraft block type identified by its registry key (e.g. {@code minecraft:stone}).
 *
 * <p>Types are flyweights resolved from the loaded server registry; obtain instances
 * via {@link BlockTypes#of(Key)} or the named constants on {@code BlockTypes}.
 */
public interface BlockType {

    /** Registry key, e.g. {@code minecraft:stone}. */
    Key key();
}
