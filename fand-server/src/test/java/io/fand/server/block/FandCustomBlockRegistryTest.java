package io.fand.server.block;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.block.Block;
import io.fand.api.block.BlockEntity;
import io.fand.api.block.BlockType;
import io.fand.api.block.component.BlockComponentKeys;
import io.fand.api.component.DataComponentContainer;
import io.fand.api.component.DataComponentKey;
import io.fand.api.component.DataComponentMap;
import io.fand.api.customblock.CustomBlockContext;
import io.fand.api.customblock.CustomBlockListener;
import io.fand.api.customblock.CustomBlockType;
import io.fand.api.event.Event;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventListener;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.world.ChunkSnapshot;
import io.fand.api.world.Difficulty;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.api.world.WorldBorder;
import io.fand.api.world.particle.ParticleEffect;
import io.fand.api.world.particle.ParticleEmission;
import io.fand.api.world.sound.SoundEffect;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class FandCustomBlockRegistryTest {

    private static final Key MACHINE_ID = Key.key("test:machine");
    private static final BlockType STONE = new TestBlockType(Key.key("minecraft:stone"));

    @Test
    void placesAndTicksCustomBlocks() {
        var world = new TestWorld();
        var block = world.blockAt(1, 64, 1);
        var events = new ArrayList<String>();
        var registry = new FandCustomBlockRegistry(new NoopEventBus());
        registry.register(new CustomBlockType(MACHINE_ID, STONE, DataComponentMap.EMPTY, true), new CustomBlockListener() {
            @Override
            public void placed(CustomBlockContext context) {
                events.add("placed:" + context.type().id().asString());
            }

            @Override
            public void tick(CustomBlockContext context) {
                events.add("tick:" + context.block().x());
            }
        });

        assertThat(registry.place(block, MACHINE_ID)).isTrue();
        registry.tick(world);

        assertThat(block.components().get(BlockComponentKeys.CUSTOM_ID)).contains(MACHINE_ID);
        assertThat(block.components().get(BlockComponentKeys.TICKING)).contains(true);
        assertThat(registry.tickingBlocks(world, 0, 0)).containsExactly(block);
        assertThat(events).containsExactly("placed:test:machine", "tick:1");
    }

    @Test
    void removeFiresBrokenAndClearsComponents() {
        var world = new TestWorld();
        var block = world.blockAt(1, 64, 1);
        var events = new ArrayList<String>();
        var registry = new FandCustomBlockRegistry(new NoopEventBus());
        registry.register(new CustomBlockType(MACHINE_ID, STONE), new CustomBlockListener() {
            @Override
            public void broken(CustomBlockContext context) {
                events.add("broken");
            }
        });

        registry.place(block, MACHINE_ID);

        assertThat(registry.remove(block)).isTrue();
        assertThat(block.components().snapshot().isEmpty()).isTrue();
        assertThat(events).containsExactly("broken");
    }

    @Test
    void itemBindingsAreIndependentRegistrations() {
        var registry = new FandCustomBlockRegistry(new NoopEventBus());
        var registration = registry.register(new CustomBlockType(MACHINE_ID, STONE));
        var binding = registration.bindItem(Key.key("test:machine_item"));

        assertThat(registry.blockForItem(Key.key("test:machine_item")).map(CustomBlockType::id)).contains(MACHINE_ID);

        binding.unregister();

        assertThat(registry.type(MACHINE_ID)).isPresent();
        assertThat(registry.blockForItem(Key.key("test:machine_item"))).isEmpty();
    }

    private record TestBlockType(Key key) implements BlockType {
    }

    private static final class TestWorld implements World {

        private final Map<String, TestBlock> blocks = new java.util.concurrent.ConcurrentHashMap<>();

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
            return new TestWorldBorder();
        }

        @Override
        public CompletableFuture<Boolean> save() {
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public Collection<? extends io.fand.api.entity.Player> players() {
            return List.of();
        }

        @Override
        public boolean chunkLoaded(int chunkX, int chunkZ) {
            return true;
        }

        @Override
        public ChunkSnapshot chunkSnapshot(int chunkX, int chunkZ) {
            return new ChunkSnapshot(this, chunkX, chunkZ, true, false, 0);
        }

        @Override
        public void playSound(Location location, SoundEffect sound) {
        }

        @Override
        public void spawnParticle(Location location, ParticleEffect effect, ParticleEmission emission) {
        }

        @Override
        public Collection<? extends Block> blocksWith(DataComponentKey<?> key) {
            return blocks.values().stream().filter(block -> block.components().has(key)).toList();
        }

        @Override
        public Collection<? extends Block> blocksWith(DataComponentKey<?> key, int chunkX, int chunkZ) {
            return blocks.values().stream()
                    .filter(block -> (block.x() >> 4) == chunkX && (block.z() >> 4) == chunkZ)
                    .filter(block -> block.components().has(key))
                    .toList();
        }

        @Override
        public Block blockAt(int x, int y, int z) {
            return blocks.computeIfAbsent(x + "," + y + "," + z, ignored -> new TestBlock(this, x, y, z));
        }

        @Override
        public Iterable<? extends Audience> audiences() {
            return List.of();
        }
    }

    private static final class TestWorldBorder implements WorldBorder {

        @Override public double centerX() { return 0; }
        @Override public double centerZ() { return 0; }
        @Override public void setCenter(double x, double z) { }
        @Override public double size() { return 0; }
        @Override public double targetSize() { return 0; }
        @Override public long remainingTransitionTicks() { return 0; }
        @Override public void setSize(double size) { }
        @Override public void setSize(double size, java.time.Duration transition) { }
        @Override public int warningDistance() { return 0; }
        @Override public void setWarningDistance(int blocks) { }
        @Override public int warningTime() { return 0; }
        @Override public void setWarningTime(int seconds) { }
        @Override public double damageBuffer() { return 0; }
        @Override public void setDamageBuffer(double blocks) { }
        @Override public double damageAmount() { return 0; }
        @Override public void setDamageAmount(double damagePerBlock) { }
        @Override public boolean contains(double x, double z) { return true; }
    }

    private static final class TestBlock implements Block {

        private final TestWorld world;
        private final int x;
        private final int y;
        private final int z;
        private final TestComponents components = new TestComponents();
        private BlockType type = STONE;

        private TestBlock(TestWorld world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public World world() {
            return world;
        }

        @Override
        public int x() {
            return x;
        }

        @Override
        public int y() {
            return y;
        }

        @Override
        public int z() {
            return z;
        }

        @Override
        public BlockType type() {
            return type;
        }

        @Override
        public Optional<? extends BlockEntity> blockEntity() {
            return Optional.empty();
        }

        @Override
        public boolean setType(BlockType type) {
            this.type = type;
            return true;
        }

        @Override
        public boolean setType(BlockType type, DataComponentMap components) {
            this.type = type;
            this.components.replace(components);
            return true;
        }

        @Override
        public DataComponentContainer components() {
            return components;
        }
    }

    private static final class TestComponents implements DataComponentContainer {

        private DataComponentMap map = DataComponentMap.EMPTY;

        @Override
        public DataComponentMap snapshot() {
            return map;
        }

        @Override
        public void set(Key key, com.google.gson.JsonElement value) {
            map = map.with(key, value);
        }

        @Override
        public void remove(Key key) {
            map = map.without(key);
        }

        @Override
        public void clear() {
            map = DataComponentMap.EMPTY;
        }

        private void replace(DataComponentMap map) {
            this.map = map;
        }
    }

    private static final class NoopEventBus implements EventBus {

        @Override
        public <E extends Event> EventSubscription subscribe(Class<E> type, EventPriority priority, EventListener<E> listener) {
            return new EventSubscription() {
                @Override
                public boolean active() {
                    return true;
                }

                @Override
                public void unregister() {
                }
            };
        }

        @Override
        public <E extends Event> E fire(E event) {
            return event;
        }

        @Override
        public boolean hasListeners(Class<? extends Event> type) {
            return false;
        }

        @Override
        public <E extends Event> CompletableFuture<E> fireAsync(E event, java.util.concurrent.Executor executor) {
            return CompletableFuture.completedFuture(event);
        }
    }
}
