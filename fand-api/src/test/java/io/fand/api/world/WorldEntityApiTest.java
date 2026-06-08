package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.entity.Entity;
import io.fand.api.entity.EntityType;
import io.fand.api.entity.ItemEntity;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class WorldEntityApiTest {

    private static final TestEntityType ZOMBIE = new TestEntityType(Key.key("minecraft:zombie"), true, false);
    private static final TestEntityType COW = new TestEntityType(Key.key("minecraft:cow"), true, false);

    @Test
    void filtersEntitySnapshotsByType() {
        var zombie = new TestEntity(ZOMBIE);
        var cow = new TestEntity(COW);
        var world = new TestWorld(List.of(zombie, cow));

        var zombies = List.copyOf(world.entities(ZOMBIE));
        var nearbyCows = List.copyOf(world.nearbyEntities(world.at(0, 64, 0), 8.0, COW));

        assertThat(zombies).hasSize(1);
        assertThat(zombies.getFirst()).isSameAs(zombie);
        assertThat(nearbyCows).hasSize(1);
        assertThat(nearbyCows.getFirst()).isSameAs(cow);
    }

    @Test
    void defaultDropItemFailsWhenImplementationDoesNotSupportIt() {
        var world = new TestWorld(List.of());

        assertThat(world.dropItem(world.at(0, 64, 0), ItemStack.EMPTY).isCompletedExceptionally()).isTrue();
    }

    @Test
    void dropItemOverloadBuildsPlainStack() {
        var world = new TestWorld(List.of(), true);
        var diamond = new TestItemType(Key.key("minecraft:diamond"), 64);

        world.dropItem(world.at(0, 64, 0), diamond, 3).join();

        assertThat(world.lastDropped).isEqualTo(diamond.stack(3));
    }

    @Test
    void dropItemOptionsOverloadBuildsPlainStack() {
        var world = new TestWorld(List.of(), true);
        var diamond = new TestItemType(Key.key("minecraft:diamond"), 64);
        var options = io.fand.api.entity.EntitySpawnOptions.builder()
                .pickupDelay(20)
                .unlimitedLifetime(true)
                .build();

        world.dropItem(world.at(0, 64, 0), diamond, 3, options).join();

        assertThat(world.lastDropped).isEqualTo(diamond.stack(3));
        assertThat(world.lastOptions).isSameAs(options);
    }

    @Test
    void defaultWorldQueriesUseLoadedEntitySnapshots() {
        var zombie = new TestEntity(ZOMBIE, 0.0, 64.0, 0.0, 0.6, 1.8);
        var cow = new TestEntity(COW, 10.0, 64.0, 0.0, 0.9, 1.4);
        var world = new TestWorld(List.of(zombie, cow));

        assertThat(List.<Entity>copyOf(world.entitiesInBox(world.at(-1, 63, -1), world.at(1, 66, 1))))
                .containsExactly(zombie);
        assertThat(List.<Entity>copyOf(world.entitiesInBox(world.at(-1, 63, -1), world.at(1, 66, 1), COW))).isEmpty();
        assertThat(world.nearestEntity(world.at(9, 64, 0), 5.0).orElseThrow()).isSameAs(cow);
        assertThat(world.nearestEntity(world.at(9, 64, 0), 5.0, ZOMBIE)).isEmpty();
        assertThat(world.loadedEntityCount()).isEqualTo(2);
    }

    @Test
    void defaultWorldToolsRemainOptInForImplementations() {
        var world = new TestWorld(List.of());

        assertThat(world.rayTraceBlock(world.at(0, 64, 0), new Vector3(1, 0, 0), 4.0)).isEmpty();
        assertThat(world.rayTraceEntity(world.at(0, 64, 0), new Vector3(1, 0, 0), 4.0)).isEmpty();
        assertThat(world.chunkLoaded(0, 0)).isFalse();
        assertThat(world.entityCount(0, 0)).isZero();
        assertThat(world.strikeLightning(world.at(0, 64, 0)).isCompletedExceptionally()).isTrue();
        assertThat(world.createExplosion(world.at(0, 64, 0), 2.0F).isCompletedExceptionally()).isTrue();
    }

    private record TestEntityType(Key key, boolean spawnable, boolean player) implements EntityType {
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private record TestEntity(EntityType type, double x, double y, double z, double width, double height) implements Entity {

        private TestEntity(EntityType type) {
            this(type, 0.0, 64.0, 0.0, 0.6, 1.8);
        }

        @Override
        public UUID uniqueId() {
            return UUID.randomUUID();
        }

        @Override
        public int entityId() {
            return 0;
        }

        @Override
        public boolean alive() {
            return true;
        }

        @Override
        public Location location() {
            return world().at(x, y, z);
        }

        @Override
        public World world() {
            return new TestWorld(List.of(this));
        }

        @Override
        public Vector3 velocity() {
            return new Vector3(0, 0, 0);
        }

        @Override
        public void setVelocity(Vector3 velocity) {
        }

        @Override
        public Optional<Component> customName() {
            return Optional.empty();
        }

        @Override
        public void setCustomName(Component name) {
        }

        @Override
        public boolean customNameVisible() {
            return false;
        }

        @Override
        public void setCustomNameVisible(boolean visible) {
        }

        @Override
        public boolean glowing() {
            return false;
        }

        @Override
        public void setGlowing(boolean glowing) {
        }

        @Override
        public boolean silent() {
            return false;
        }

        @Override
        public void setSilent(boolean silent) {
        }

        @Override
        public boolean gravity() {
            return true;
        }

        @Override
        public void setGravity(boolean gravity) {
        }

        @Override
        public boolean invulnerable() {
            return false;
        }

        @Override
        public void setInvulnerable(boolean invulnerable) {
        }

        @Override
        public Set<String> scoreboardTags() {
            return Set.of();
        }

        @Override
        public void addScoreboardTag(String tag) {
        }

        @Override
        public void removeScoreboardTag(String tag) {
        }

        @Override
        public double width() {
            return width;
        }

        @Override
        public double height() {
            return height;
        }

        @Override
        public CompletableFuture<Boolean> teleport(Location destination) {
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public void remove() {
        }

        @Override
        public Optional<? extends Entity> vehicle() {
            return Optional.empty();
        }

        @Override
        public List<? extends Entity> passengers() {
            return List.of();
        }

        @Override
        public CompletableFuture<Boolean> mount(Entity vehicle) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public CompletableFuture<Boolean> addPassenger(Entity passenger) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public CompletableFuture<Boolean> removePassenger(Entity passenger) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public CompletableFuture<Boolean> dismount() {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public void ejectPassengers() {
        }

        @Override
        public boolean onGround() {
            return true;
        }

        @Override
        public boolean inWater() {
            return false;
        }

        @Override
        public boolean inLava() {
            return false;
        }

        @Override
        public int fireTicks() {
            return 0;
        }

        @Override
        public void setFireTicks(int ticks) {
        }

        @Override
        public int ticksLived() {
            return 0;
        }

        @Override
        public io.fand.api.component.DataComponentContainer components() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestWorld implements World {

        private final List<Entity> entities;
        private final boolean supportsDrop;
        private ItemStack lastDropped;
        private io.fand.api.entity.EntitySpawnOptions lastOptions;

        private TestWorld(List<Entity> entities) {
            this(entities, false);
        }

        private TestWorld(List<Entity> entities, boolean supportsDrop) {
            this.entities = entities;
            this.supportsDrop = supportsDrop;
        }

        @Override
        public Key key() {
            return Key.key("minecraft:overworld");
        }

        @Override
        public long seed() {
            return 0;
        }

        @Override
        public long gameTime() {
            return 0;
        }

        @Override
        public CompletableFuture<Void> setGameTime(long ticks) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public long time() {
            return 0;
        }

        @Override
        public CompletableFuture<Void> setTime(long ticks) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Difficulty difficulty() {
            return Difficulty.NORMAL;
        }

        @Override
        public CompletableFuture<Void> setDifficulty(Difficulty difficulty) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean storm() {
            return false;
        }

        @Override
        public CompletableFuture<Void> setStorm(boolean storm) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean thundering() {
            return false;
        }

        @Override
        public CompletableFuture<Void> setThundering(boolean thundering) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public WorldBorder worldBorder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Boolean> save() {
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public Collection<? extends Player> players() {
            return List.of();
        }

        @Override
        public Collection<? extends Entity> entities() {
            return entities;
        }

        @Override
        public Collection<? extends Entity> nearbyEntities(Location center, double radius) {
            return entities;
        }

        @Override
        public Iterable<? extends Audience> audiences() {
            return List.of();
        }

        @Override
        public void playSound(Location location, io.fand.api.world.sound.SoundEffect sound) {
        }

        @Override
        public void spawnParticle(
                Location location,
                io.fand.api.world.particle.ParticleEffect effect,
                io.fand.api.world.particle.ParticleEmission emission) {
        }

        @Override
        public io.fand.api.block.Block blockAt(int x, int y, int z) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Optional<? extends ItemEntity>> dropItem(Location location, ItemStack item) {
            return dropItem(location, item, io.fand.api.entity.EntitySpawnOptions.defaults());
        }

        @Override
        public CompletableFuture<Optional<? extends ItemEntity>> dropItem(
                Location location,
                ItemStack item,
                io.fand.api.entity.EntitySpawnOptions options
        ) {
            if (supportsDrop) {
                this.lastDropped = item;
                this.lastOptions = options;
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return World.super.dropItem(location, item, options);
        }
    }
}
