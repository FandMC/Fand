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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
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

    private record TestEntityType(Key key, boolean spawnable, boolean player) implements EntityType {
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private record TestEntity(EntityType type) implements Entity {

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
            return world().at(0, 64, 0);
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
            if (supportsDrop) {
                this.lastDropped = item;
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return World.super.dropItem(location, item);
        }
    }
}
