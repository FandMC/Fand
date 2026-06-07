package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class SoundApiTest {

    @Test
    void commonSoundConstantsAreFinalApiFields() {
        for (var field : Sounds.class.getFields()) {
            if (field.getType() == Sound.class) {
                assertThat(Modifier.isFinal(field.getModifiers()))
                        .as(field.getName())
                        .isTrue();
            }
        }
    }

    @Test
    void keyStringFactoryUsesSameCachedSoundAsKeyFactory() {
        var parsedKey = Key.key("minecraft:block.note_block.pling");

        assertThat(Sounds.key("minecraft:block.note_block.pling"))
                .isSameAs(Sounds.sound(parsedKey))
                .isSameAs(Sounds.BLOCK_NOTE_BLOCK_PLING);
    }

    @Test
    void defaultPlaybackSeedIsRandomizedForVariantSelection() {
        var seeds = new HashSet<Long>();
        for (var i = 0; i < 32; i++) {
            seeds.add(Sounds.BLOCK_NOTE_BLOCK_PLING.at(0.0, 0.0, 0.0).seed());
        }

        assertThat(seeds).hasSizeGreaterThan(1);
    }

    @Test
    void explicitSeedCanBeUsedForDeterministicVariantSelection() {
        var playback = Sounds.BLOCK_NOTE_BLOCK_PLING.at(0.0, 0.0, 0.0).seed(0L);

        assertThat(playback.seed()).isEqualTo(0L);
    }

    @Test
    void rejectsNonFiniteCoordinates() {
        assertThatThrownBy(() -> SoundPlayback.of(Sounds.BLOCK_NOTE_BLOCK_PLING, Double.NaN, 0.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("x must be finite");
        assertThatThrownBy(() -> SoundPlayback.of(Sounds.BLOCK_NOTE_BLOCK_PLING, 0.0, Double.POSITIVE_INFINITY, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("y must be finite");
        assertThatThrownBy(() -> SoundPlayback.of(Sounds.BLOCK_NOTE_BLOCK_PLING, 0.0, 0.0, Double.NEGATIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("z must be finite");
    }

    @Test
    void rejectsNonFinitePlaybackFloats() {
        var playback = Sounds.BLOCK_NOTE_BLOCK_PLING.at(0.0, 0.0, 0.0);

        assertThatThrownBy(() -> playback.volume(Float.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volume must be finite");
        assertThatThrownBy(() -> playback.pitch(Float.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pitch must be finite");
        assertThatThrownBy(() -> playback.minVolume(Float.NEGATIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minVolume must be finite");
    }
}
