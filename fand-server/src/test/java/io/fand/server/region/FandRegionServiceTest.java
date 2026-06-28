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

    @Test
    void priorityBeatsVolumeWhenSelectingApplicableRegion() {
        var service = new FandRegionService(tempDir.resolve("priority-regions"));
        var world = Key.key("minecraft:overworld");
        var lowPrioritySmall = RegionDefinition.builder(
                        Key.key("fand:small"),
                        world,
                        new BlockRegion(0, 0, 0, 2, 2, 2))
                .build();
        var highPriorityLarge = RegionDefinition.builder(
                        Key.key("fand:large"),
                        world,
                        new BlockRegion(0, 0, 0, 10, 10, 10))
                .priority(10)
                .build();

        service.register(lowPrioritySmall);
        service.register(highPriorityLarge);

        var location = new Location(new TestWorld(world), 1, 1, 1, 0.0F, 0.0F);

        assertThat(service.applicableRegions(location))
                .extracting(Region::key)
                .containsExactly(Key.key("fand:large"), Key.key("fand:small"));
        assertThat(service.applicableRegion(location)).map(Region::key).contains(Key.key("fand:large"));
    }

    @Test
    void samePriorityUsesSmallerVolumeThenNewestRegistration() {
        var service = new FandRegionService(tempDir.resolve("same-priority-regions"));
        var world = Key.key("minecraft:overworld");
        var large = RegionDefinition.builder(
                        Key.key("fand:large"),
                        world,
                        new BlockRegion(0, 0, 0, 20, 20, 20))
                .priority(5)
                .build();
        var olderSmall = RegionDefinition.builder(
                        Key.key("fand:older-small"),
                        world,
                        new BlockRegion(0, 0, 0, 5, 5, 5))
                .priority(5)
                .build();
        var newerSmall = RegionDefinition.builder(
                        Key.key("fand:newer-small"),
                        world,
                        new BlockRegion(0, 0, 0, 5, 5, 5))
                .priority(5)
                .build();

        service.register(large);
        service.register(olderSmall);
        service.register(newerSmall);

        var location = new Location(new TestWorld(world), 1, 1, 1, 0.0F, 0.0F);

        assertThat(service.applicableRegions(location))
                .extracting(Region::key)
                .containsExactly(
                        Key.key("fand:newer-small"),
                        Key.key("fand:older-small"),
                        Key.key("fand:large"));
    }

    @Test
    void resolvesFlagsThroughParentTraceBeforeLowerPriorityRegions() {
        var service = new FandRegionService(tempDir.resolve("trace-regions"));
        var world = Key.key("minecraft:overworld");
        var flag = RegionFlag.bool(Key.key("fand:pvp"), false);
        var parent = RegionDefinition.builder(
                        Key.key("fand:parent"),
                        world,
                        new BlockRegion(100, 0, 100, 110, 10, 110))
                .flag(flag, true)
                .build();
        var lower = RegionDefinition.builder(
                        Key.key("fand:lower"),
                        world,
                        new BlockRegion(0, 0, 0, 10, 10, 10))
                .flag(flag, false)
                .build();
        var child = RegionDefinition.builder(
                        Key.key("fand:child"),
                        world,
                        new BlockRegion(0, 0, 0, 10, 10, 10))
                .priority(5)
                .parent(parent.key())
                .build();

        service.register(parent);
        service.register(lower);
        service.register(child);

        var location = new Location(new TestWorld(world), 5, 5, 5, 0.0F, 0.0F);
        var resolution = service.resolveFlag(location, flag);

        assertThat(resolution.value()).contains(true);
        assertThat(resolution.defaultValue()).isFalse();
        assertThat(resolution.trace()).extracting(trace -> trace.region().key())
                .containsExactly(Key.key("fand:child"), Key.key("fand:parent"));
        assertThat(resolution.trace().get(0).inherited()).isFalse();
        assertThat(resolution.trace().get(1).inherited()).isTrue();
    }

    @Test
    void persistsAndReloadsProtectionMetadata() throws Exception {
        var service = new FandRegionService(tempDir.resolve("protected-regions"));
        var region = RegionDefinition.builder(
                        Key.key("fand:protected"),
                        Key.key("minecraft:overworld"),
                        new BlockRegion(0, 0, 0, 10, 10, 10))
                .priority(7)
                .parent(Key.key("fand:parent"))
                .owner("User:Alice")
                .member("Group:Builders")
                .build();

        service.register(region);

        var reloaded = new FandRegionService(tempDir.resolve("protected-regions"));
        var loaded = reloaded.region(Key.key("fand:protected")).orElseThrow();

        assertThat(loaded.protection().priority()).isEqualTo(7);
        assertThat(loaded.protection().parent()).contains(Key.key("fand:parent"));
        assertThat(loaded.protection().owner("user:alice")).isTrue();
        assertThat(loaded.protection().member("group:builders")).isTrue();
        assertThat(Files.readString(tempDir.resolve("protected-regions/fand/protected.json")))
                .contains("\"protection\"", "\"priority\": 7");
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
