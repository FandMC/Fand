package io.fand.server.world;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.world.sound.SoundCategory;
import io.fand.api.world.sound.Sounds;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;

final class SoundEffectsTest {

    @Test
    void resolveSourceMapsEveryFandCategory() {
        assertThat(SoundEffects.resolveSource(SoundCategory.MASTER)).isEqualTo(SoundSource.MASTER);
        assertThat(SoundEffects.resolveSource(SoundCategory.MUSIC)).isEqualTo(SoundSource.MUSIC);
        assertThat(SoundEffects.resolveSource(SoundCategory.RECORD)).isEqualTo(SoundSource.RECORDS);
        assertThat(SoundEffects.resolveSource(SoundCategory.WEATHER)).isEqualTo(SoundSource.WEATHER);
        assertThat(SoundEffects.resolveSource(SoundCategory.BLOCK)).isEqualTo(SoundSource.BLOCKS);
        assertThat(SoundEffects.resolveSource(SoundCategory.HOSTILE)).isEqualTo(SoundSource.HOSTILE);
        assertThat(SoundEffects.resolveSource(SoundCategory.NEUTRAL)).isEqualTo(SoundSource.NEUTRAL);
        assertThat(SoundEffects.resolveSource(SoundCategory.PLAYER)).isEqualTo(SoundSource.PLAYERS);
        assertThat(SoundEffects.resolveSource(SoundCategory.AMBIENT)).isEqualTo(SoundSource.AMBIENT);
        assertThat(SoundEffects.resolveSource(SoundCategory.VOICE)).isEqualTo(SoundSource.VOICE);
    }

    @Test
    void seedOfDefaultsToZeroAndKeepsExplicitSeed() {
        var unseeded = Sounds.effect("minecraft:block.note_block.pling", SoundCategory.RECORD);
        var seeded = unseeded.withSeed(42L);

        assertThat(SoundEffects.seedOf(unseeded)).isEqualTo(0L);
        assertThat(SoundEffects.seedOf(seeded)).isEqualTo(42L);
    }
}
