package io.fand.api.world.sound;

import java.util.OptionalLong;
import net.kyori.adventure.key.Key;

/** Immutable sound playback description. */
public record SoundEffect(Key key, SoundCategory category, float volume, float pitch, OptionalLong seed) {

    public SoundEffect {
        java.util.Objects.requireNonNull(key, "key");
        java.util.Objects.requireNonNull(category, "category");
        java.util.Objects.requireNonNull(seed, "seed");
        if (!Float.isFinite(volume) || volume < 0.0F) {
            throw new IllegalArgumentException("volume must be finite and >= 0");
        }
        if (!Float.isFinite(pitch) || pitch < 0.0F) {
            throw new IllegalArgumentException("pitch must be finite and >= 0");
        }
    }

    public static SoundEffect of(Key key, SoundCategory category) {
        return new SoundEffect(key, category, 1.0F, 1.0F, OptionalLong.empty());
    }

    public static SoundEffect of(SoundKey key, SoundCategory category) {
        return of(key.key(), category);
    }

    public static SoundEffect of(String key, SoundCategory category) {
        return of(Key.key(key), category);
    }

    public SoundEffect withVolume(float volume) {
        return new SoundEffect(key, category, volume, pitch, seed);
    }

    public SoundEffect withPitch(float pitch) {
        return new SoundEffect(key, category, volume, pitch, seed);
    }

    public SoundEffect withSeed(long seed) {
        return new SoundEffect(key, category, volume, pitch, OptionalLong.of(seed));
    }

    public SoundEffect withoutSeed() {
        return new SoundEffect(key, category, volume, pitch, OptionalLong.empty());
    }
}
