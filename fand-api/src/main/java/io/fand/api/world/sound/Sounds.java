package io.fand.api.world.sound;

import net.kyori.adventure.key.Key;

/** Factory methods for sound effects. */
public final class Sounds {

    private Sounds() {
    }

    public static SoundEffect effect(Key key, SoundCategory category) {
        return SoundEffect.of(key, category);
    }

    public static SoundEffect effect(SoundKey key, SoundCategory category) {
        return SoundEffect.of(key, category);
    }

    public static SoundEffect effect(String key, SoundCategory category) {
        return SoundEffect.of(key, category);
    }
}
