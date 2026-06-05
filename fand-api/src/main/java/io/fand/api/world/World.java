package io.fand.api.world;

import net.kyori.adventure.key.Key;

/**
 * A loaded world (dimension) on the server. Identified by a {@link Key} matching
 * the underlying Minecraft dimension key (e.g. {@code minecraft:overworld}).
 *
 * <p>World handles are stable for as long as the dimension stays loaded; equality
 * is by {@link #key()}. Methods that touch world state must be called on the main
 * thread unless explicitly documented as thread-safe.
 */
public interface World {

    /** Dimension key, e.g. {@code minecraft:overworld}. */
    Key key();

    /** Convenience for {@code key().asString()}. */
    default String name() {
        return key().asString();
    }

    /** World seed. */
    long seed();

    /** Builds a {@link Location} in this world. */
    default Location at(double x, double y, double z) {
        return new Location(this, x, y, z, 0.0F, 0.0F);
    }

    /** Builds a {@link Location} in this world with rotation. */
    default Location at(double x, double y, double z, float yaw, float pitch) {
        return new Location(this, x, y, z, yaw, pitch);
    }
}
