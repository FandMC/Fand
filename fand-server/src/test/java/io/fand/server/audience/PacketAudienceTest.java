package io.fand.server.audience;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;

class PacketAudienceTest {

    @Test
    void resolveSourceMapsEveryAdventureSource() {
        assertThat(PacketAudience.resolveSource(Sound.Source.MASTER)).isEqualTo(SoundSource.MASTER);
        assertThat(PacketAudience.resolveSource(Sound.Source.MUSIC)).isEqualTo(SoundSource.MUSIC);
        assertThat(PacketAudience.resolveSource(Sound.Source.RECORD)).isEqualTo(SoundSource.RECORDS);
        assertThat(PacketAudience.resolveSource(Sound.Source.WEATHER)).isEqualTo(SoundSource.WEATHER);
        assertThat(PacketAudience.resolveSource(Sound.Source.BLOCK)).isEqualTo(SoundSource.BLOCKS);
        assertThat(PacketAudience.resolveSource(Sound.Source.HOSTILE)).isEqualTo(SoundSource.HOSTILE);
        assertThat(PacketAudience.resolveSource(Sound.Source.NEUTRAL)).isEqualTo(SoundSource.NEUTRAL);
        assertThat(PacketAudience.resolveSource(Sound.Source.PLAYER)).isEqualTo(SoundSource.PLAYERS);
        assertThat(PacketAudience.resolveSource(Sound.Source.AMBIENT)).isEqualTo(SoundSource.AMBIENT);
        assertThat(PacketAudience.resolveSource(Sound.Source.VOICE)).isEqualTo(SoundSource.VOICE);
    }

    @Test
    void seedOfReturnsExplicitSeedWhenSet() {
        var sound = Sound.sound()
                .type(Key.key("entity.experience_orb.pickup"))
                .source(Sound.Source.PLAYER)
                .volume(1.0F)
                .pitch(1.0F)
                .seed(42L)
                .build();
        assertThat(PacketAudience.seedOf(sound)).isEqualTo(42L);
    }

    @Test
    void seedOfDefaultsToZeroWhenAbsent() {
        var sound = Sound.sound(Key.key("entity.experience_orb.pickup"), Sound.Source.PLAYER, 1.0F, 1.0F);
        assertThat(sound.seed()).isEmpty();
        assertThat(PacketAudience.seedOf(sound)).isEqualTo(0L);
    }

    @Test
    void ticksConvertsMillisAtTwentyTicksPerSecond() {
        assertThat(PacketAudience.ticks(Duration.ofSeconds(1).toMillis())).isEqualTo(20);
        assertThat(PacketAudience.ticks(Duration.ofMillis(500).toMillis())).isEqualTo(10);
        assertThat(PacketAudience.ticks(0)).isEqualTo(0);
    }

    @Test
    void ticksClampsNegativeMillisToZero() {
        assertThat(PacketAudience.ticks(-1)).isEqualTo(0);
        assertThat(PacketAudience.ticks(-1000)).isEqualTo(0);
    }
}
