package io.fand.api.world.sound;

import java.util.Objects;
import java.util.OptionalLong;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/** Immutable sound playback description. */
public final class SoundEffect {

    private final Key key;
    private final SoundCategory category;
    private final float volume;
    private final float pitch;
    private final @Nullable Long seed;

    public SoundEffect(Key key, SoundCategory category, float volume, float pitch, @Nullable Long seed) {
        this.key = Objects.requireNonNull(key, "key");
        this.category = Objects.requireNonNull(category, "category");
        if (!Float.isFinite(volume) || volume < 0.0F) {
            throw new IllegalArgumentException("volume must be finite and >= 0");
        }
        if (!Float.isFinite(pitch) || pitch < 0.0F) {
            throw new IllegalArgumentException("pitch must be finite and >= 0");
        }
        this.volume = volume;
        this.pitch = pitch;
        this.seed = seed;
    }

    public Key key() {
        return key;
    }

    public SoundCategory category() {
        return category;
    }

    public float volume() {
        return volume;
    }

    public float pitch() {
        return pitch;
    }

    public OptionalLong seed() {
        return seed == null ? OptionalLong.empty() : OptionalLong.of(seed);
    }

    public static SoundEffect of(Key key, SoundCategory category) {
        return new SoundEffect(key, category, 1.0F, 1.0F, null);
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
        return new SoundEffect(key, category, volume, pitch, seed);
    }

    public SoundEffect withoutSeed() {
        return new SoundEffect(key, category, volume, pitch, null);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof SoundEffect that
                && Float.compare(volume, that.volume) == 0
                && Float.compare(pitch, that.pitch) == 0
                && Objects.equals(key, that.key)
                && category == that.category
                && Objects.equals(seed, that.seed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, category, volume, pitch, seed);
    }

    @Override
    public String toString() {
        return "SoundEffect[key=" + key + ", category=" + category + ", volume=" + volume
                + ", pitch=" + pitch + ", seed=" + seed + "]";
    }
}
