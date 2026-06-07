package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.block.Block;
import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class LocationTest {

    private final World world = new TestWorld();

    @Test
    void rejectsNonFiniteCoordinatesAndRotation() {
        assertThatThrownBy(() -> new Location(world, Double.NaN, 0.0, 0.0, 0.0F, 0.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("x must be finite");
        assertThatThrownBy(() -> new Location(world, 0.0, Double.POSITIVE_INFINITY, 0.0, 0.0F, 0.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("y must be finite");
        assertThatThrownBy(() -> new Location(world, 0.0, 0.0, Double.NEGATIVE_INFINITY, 0.0F, 0.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("z must be finite");
        assertThatThrownBy(() -> new Location(world, 0.0, 0.0, 0.0, Float.NaN, 0.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yaw must be finite");
        assertThatThrownBy(() -> new Location(world, 0.0, 0.0, 0.0, 0.0F, Float.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pitch must be finite");
    }

    @Test
    void offsetRevalidatesResultingCoordinates() {
        var location = new Location(world, Double.MAX_VALUE, 0.0, 0.0, 0.0F, 0.0F);

        assertThatThrownBy(() -> location.offset(Double.MAX_VALUE, 0.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("x must be finite");
    }

    @Test
    void blockCoordinatesFloorValues() {
        var location = new Location(world, -1.2, 64.8, 2.0, 0.0F, 0.0F);

        assertThat(location.blockX()).isEqualTo(-2);
        assertThat(location.blockY()).isEqualTo(64);
        assertThat(location.blockZ()).isEqualTo(2);
    }

    private static final class TestWorld implements World {

        @Override
        public Key key() {
            return Key.key("test:world");
        }

        @Override
        public long seed() {
            return 0L;
        }

        @Override
        public Collection<? extends Player> players() {
            return List.of();
        }

        @Override
        public Collection<? extends Entity> entities() {
            return List.of();
        }

        @Override
        public Iterable<? extends Audience> audiences() {
            return List.of();
        }

        @Override
        public Block blockAt(int x, int y, int z) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void spawnParticle(ParticlePlayback playback) {
        }

        @Override
        public void playSound(SoundPlayback playback) {
        }

        @Override
        public long time() {
            return 0L;
        }

        @Override
        public void setTime(long time) {
        }

        @Override
        public boolean dayNightCycleEnabled() {
            return true;
        }

        @Override
        public void setDayNightCycleEnabled(boolean enabled) {
        }

        @Override
        public boolean storming() {
            return false;
        }

        @Override
        public void setStorming(boolean storming) {
        }

        @Override
        public void setStorming(boolean storming, Duration duration) {
        }

        @Override
        public void setStorming(boolean storming, int ticks) {
        }

        @Override
        public boolean thundering() {
            return false;
        }

        @Override
        public void setThundering(boolean thundering) {
        }

        @Override
        public void setThundering(boolean thundering, Duration duration) {
        }

        @Override
        public void setThundering(boolean thundering, int ticks) {
        }
    }
}
