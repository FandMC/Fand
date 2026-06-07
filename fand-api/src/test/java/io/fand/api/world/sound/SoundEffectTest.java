package io.fand.api.world.sound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.OptionalLong;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class SoundEffectTest {

    @Test
    void buildsImmutableSoundEffects() {
        var sound = Sounds.effect(SoundKey.EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYER)
                .withVolume(0.75F)
                .withPitch(1.25F)
                .withSeed(42L);

        assertThat(sound.key()).isEqualTo(Key.key("minecraft:entity.experience_orb.pickup"));
        assertThat(sound.category()).isEqualTo(SoundCategory.PLAYER);
        assertThat(sound.volume()).isEqualTo(0.75F);
        assertThat(sound.pitch()).isEqualTo(1.25F);
        assertThat(sound.seed()).isEqualTo(OptionalLong.of(42L));
        assertThat(sound.withoutSeed().seed()).isEmpty();
        assertThat(Sounds.effect("minecraft:ui.toast.challenge_complete", SoundCategory.MASTER).key())
                .isEqualTo(SoundKey.UI_TOAST_CHALLENGE_COMPLETE.key());
    }

    @Test
    void rejectsInvalidSoundValues() {
        assertThatThrownBy(() -> Sounds.effect("minecraft:block.note_block.pling", SoundCategory.RECORD)
                .withVolume(-1.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volume");
        assertThatThrownBy(() -> Sounds.effect("minecraft:block.note_block.pling", SoundCategory.RECORD)
                .withPitch(Float.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pitch");
    }
}
