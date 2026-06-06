package io.fand.api.world;

import net.kyori.adventure.key.Key;

/**
 * A particle effect type that can be spawned in a {@link World}.
 *
 * <p>Particles are identified by their Minecraft registry key (e.g.
 * {@code minecraft:flame}). Use {@link Particles} for common vanilla types.
 */
public interface Particle {

    /** Registry key identifying this particle type. */
    Key key();

    /**
     * Full vanilla particle argument string, including optional data after the
     * key. For simple particles this is the same as {@link #key()}.
     */
    default String argument() {
        return key().asString();
    }

    /** Builds a particle playback at {@code location}. */
    default ParticlePlayback at(Location location) {
        return ParticlePlayback.of(this, location);
    }

    /** Builds a particle playback at the given coordinates. */
    default ParticlePlayback at(double x, double y, double z) {
        return ParticlePlayback.of(this, x, y, z);
    }
}
