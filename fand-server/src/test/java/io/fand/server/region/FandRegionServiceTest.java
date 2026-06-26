package io.fand.server.region;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.region.Region;
import io.fand.api.region.RegionDefinition;
import io.fand.api.region.RegionFlag;
import io.fand.api.world.BlockRegion;
import io.fand.api.world.Difficulty;
import io.fand.api.world.Location;
import io.fand.api.world.WorldBorder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import net.kyori.adventure.key.Key;

final class FandRegionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void registersFindsPersistsAndReloadsRegions() {
        var service = new FandRegionService(tempDir.resolve("regions"));
        var flag = RegionFlag.bool(Key.key("fand:test_pvp"), false);
        var region = RegionDefinition.builder(Key.key("fand:spawn"), Key.key("minecraft:overworld"), new BlockRegion(0, 0, 0, 10, 10, 10))
                .flag(flag, true)
                .build();

        var flagRegistration = service.registerFlag(flag);
        var regionRegistration = service.register(region);

        var location = new Location(new TestWorld(Key.key("minecraft:overworld")), 5, 5, 5, 0.0F, 0.0F);
        var regionFile = tempDir.resolve("regions/fand/spawn.json");

        assertThat(service.rootDirectory()).isEqualTo(tempDir.resolve("regions").toAbsolutePath().normalize());
        assertThat(service.regions()).singleElement().extracting(Region::key).isEqualTo(Key.key("fand:spawn"));
        assertThat(service.applicableRegion(location)).map(Region::key).contains(region.key());
        assertThat(service.flag(flag.key())).isPresent();
        assertThat(service.region(Key.key("fand:spawn"))).isPresent();
        assertThat(Files.exists(regionFile)).isTrue();

        var reloaded = new FandRegionService(tempDir.resolve("regions"));
        assertThat(reloaded.region(Key.key("fand:spawn"))).isPresent();
        assertThat(reloaded.applicableRegion(location)).isPresent();

        regionRegistration.unregister();
        flagRegistration.unregister();

        assertThat(service.region(Key.key("fand:spawn"))).isEmpty();
        assertThat(Files.exists(regionFile)).isFalse();
    }

    private record TestWorld(Key key) implements io.fand.api.world.World {
        @Override
        public long seed() {
            return 0;
        }

        @Override
        public long gameTime() {
            return 0;
        }

        @Override
        public java.util.concurrent.CompletableFuture<Void> setGameTime(long ticks) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }

        @Override
        public long time() {
            return 0;
        }

        @Override
        public java.util.concurrent.CompletableFuture<Void> setTime(long ticks) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }

        @Override
        public Difficulty difficulty() {
            return Difficulty.NORMAL;
        }

        @Override
        public java.util.concurrent.CompletableFuture<Void> setDifficulty(Difficulty difficulty) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean storm() {
            return false;
        }

        @Override
        public java.util.concurrent.CompletableFuture<Void> setStorm(boolean storm) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean thundering() {
            return false;
        }

        @Override
        public java.util.concurrent.CompletableFuture<Void> setThundering(boolean thundering) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }

        @Override
        public WorldBorder worldBorder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> save() {
            return java.util.concurrent.CompletableFuture.completedFuture(true);
        }

        @Override
        public java.util.Collection<? extends io.fand.api.entity.Player> players() {
            return java.util.List.of();
        }

        @Override
        public Iterable<? extends net.kyori.adventure.audience.Audience> audiences() {
            return java.util.List.of();
        }

        @Override
        public void playSound(Location location, io.fand.api.world.sound.SoundEffect sound) {
        }

        @Override
        public void spawnParticle(Location location, io.fand.api.world.particle.ParticleEffect effect, io.fand.api.world.particle.ParticleEmission emission) {
        }

        @Override
        public io.fand.api.block.Block blockAt(int x, int y, int z) {
            throw new UnsupportedOperationException();
        }
    }
}
