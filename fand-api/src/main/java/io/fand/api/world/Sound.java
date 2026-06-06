package io.fand.api.world;

import net.kyori.adventure.key.Key;

/**
 * A sound effect that can be played in a {@link World} or to a
 * {@link io.fand.api.entity.Player}.
 *
 * <p>Sounds are identified by their Minecraft registry key (e.g.
 * {@code minecraft:entity.player.levelup}). Use {@link Sounds} for common
 * vanilla types.
 */
public interface Sound {

    /** Registry key identifying this sound. */
    Key key();

    /** Builds a positional playback at {@code location}. */
    default SoundPlayback at(Location location) {
        return SoundPlayback.of(this, location);
    }

    /** Builds a positional playback at the given coordinates. */
    default SoundPlayback at(double x, double y, double z) {
        return SoundPlayback.of(this, x, y, z);
    }

    /**
     * Sound category determining volume slider and attenuation rules. Categories
     * map to client settings (master, music, blocks, etc.).
     */
    enum Category {
        MASTER,
        MUSIC,
        RECORDS,
        WEATHER,
        BLOCKS,
        HOSTILE,
        NEUTRAL,
        PLAYERS,
        AMBIENT,
        VOICE
    }
}
