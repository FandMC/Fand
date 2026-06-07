package io.fand.api.world;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Complete parameters for playing a positional sound.
 *
 * <p>Volume controls both loudness and vanilla hearing range. Pitch controls
 * playback speed. {@code minVolume} mirrors Minecraft's {@code /playsound}
 * fallback volume: when playing directly to a player outside normal range, a
 * positive value keeps the sound audible by shifting the apparent source near
 * that player. {@code seed} controls client-side sound variant selection;
 * factory methods choose a random seed by default to match vanilla playback.
 */
public record SoundPlayback(
        Sound sound,
        Sound.Category category,
        double x,
        double y,
        double z,
        float volume,
        float pitch,
        float minVolume,
        long seed
) {

    public SoundPlayback {
        Objects.requireNonNull(sound, "sound");
        Objects.requireNonNull(category, "category");
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(volume, "volume");
        requireFinite(pitch, "pitch");
        requireFinite(minVolume, "minVolume");
        if (volume < 0.0F) {
            throw new IllegalArgumentException("volume must be >= 0, got " + volume);
        }
        if (pitch < 0.0F) {
            throw new IllegalArgumentException("pitch must be >= 0, got " + pitch);
        }
        if (minVolume < 0.0F || minVolume > 1.0F) {
            throw new IllegalArgumentException("minVolume must be in [0, 1], got " + minVolume);
        }
    }

    public static SoundPlayback of(Sound sound, Location location) {
        Objects.requireNonNull(location, "location");
        return of(sound, location.x(), location.y(), location.z());
    }

    public static SoundPlayback of(Sound sound, double x, double y, double z) {
        return new SoundPlayback(sound, Sound.Category.MASTER, x, y, z, 1.0F, 1.0F, 0.0F, ThreadLocalRandom.current().nextLong());
    }

    public SoundPlayback category(Sound.Category category) {
        return new SoundPlayback(sound, category, x, y, z, volume, pitch, minVolume, seed);
    }

    public SoundPlayback volume(float volume) {
        return new SoundPlayback(sound, category, x, y, z, volume, pitch, minVolume, seed);
    }

    public SoundPlayback pitch(float pitch) {
        return new SoundPlayback(sound, category, x, y, z, volume, pitch, minVolume, seed);
    }

    public SoundPlayback minVolume(float minVolume) {
        return new SoundPlayback(sound, category, x, y, z, volume, pitch, minVolume, seed);
    }

    public SoundPlayback seed(long seed) {
        return new SoundPlayback(sound, category, x, y, z, volume, pitch, minVolume, seed);
    }

    /** Returns a copy with a fresh random seed for client-side variant selection. */
    public SoundPlayback randomSeed() {
        return seed(ThreadLocalRandom.current().nextLong());
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite, got " + value);
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite, got " + value);
        }
    }
}
