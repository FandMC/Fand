package io.fand.server.block;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.block.Block;
import io.fand.api.block.BlockEntity;
import io.fand.api.block.BlockType;
import io.fand.api.block.component.BlockComponentKeys;
import io.fand.api.component.DataComponentContainer;
import io.fand.api.component.DataComponentKey;
import io.fand.api.component.DataComponentMap;
import io.fand.api.block.custom.CustomBlockContext;
import io.fand.api.block.custom.CustomBlockListener;
import io.fand.api.block.custom.CustomBlockType;
import io.fand.api.entity.GameMode;
import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventListener;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.event.block.BlockFace;
import io.fand.api.event.player.PlayerMainHandRightClickBlockEvent;
import io.fand.api.item.ItemType;
import io.fand.api.item.custom.CustomItemType;
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
import io.fand.server.event.EventDispatcher;
import io.fand.server.item.FandCustomItemRegistry;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FandCustomBlockRegistryTest {

    private static final Key MACHINE_ID = Key.key("test:machine");
    private static final BlockType STONE = new TestBlockType(Key.key("minecraft:stone"));

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

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

        assertThat(block.components().value(BlockComponentKeys.CUSTOM_ID)).contains(MACHINE_ID);
        assertThat(block.components().value(BlockComponentKeys.TICKING)).contains(true);
        assertThat(registry.tickingBlocks(world, 0, 0)).containsExactly(block);
        assertThat(events).containsExactly("placed:test:machine", "tick:1");
    }

    @Test
    void tickOnlyScansActiveChunksForRequestedWorld() {
        var overworld = new TestWorld(Key.key("minecraft:overworld"));
        var nether = new TestWorld(Key.key("minecraft:the_nether"));
        var overworldBlock = overworld.blockAt(1, 64, 1);
        var netherBlock = nether.blockAt(1, 64, 1);
        var events = new ArrayList<String>();
        var registry = new FandCustomBlockRegistry(new NoopEventBus());
        registry.register(new CustomBlockType(MACHINE_ID, STONE, DataComponentMap.EMPTY, true), new CustomBlockListener() {
            @Override
            public void tick(CustomBlockContext context) {
                events.add(context.block().world().key().asString());
            }
        });

        registry.place(overworldBlock, MACHINE_ID);
        registry.place(netherBlock, MACHINE_ID);
        registry.tick(overworld);

        assertThat(events).containsExactly("minecraft:overworld");
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
        assertThat(block.components().snapshot().empty()).isTrue();
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

    @Test
    void sameIdBlockItemBecomesDeterministicDefaultDrop() {
        var items = new FandCustomItemRegistry();
        var baseItem = new TestItemType(Key.key("minecraft:paper"));
        items.register(CustomItemType.of(Key.key("test:alternate_item"), baseItem));
        items.register(CustomItemType.of(MACHINE_ID, baseItem));
        var registry = new FandCustomBlockRegistry(new NoopEventBus(), items);
        registry.register(new CustomBlockType(MACHINE_ID, STONE));
        registry.bindItem(Key.key("test:alternate_item"), MACHINE_ID);
        registry.bindItem(MACHINE_ID, MACHINE_ID);

        assertThat(registry.defaultDrop(MACHINE_ID).map(stack -> stack.type().key())).contains(MACHINE_ID);
    }

    @Test
    void explosionDecayMatchesVanillaPerItemProbability() {
        var randomValues = new java.util.ArrayDeque<>(List.of(0.10, 0.25, 0.251, 0.80));

        assertThat(FandCustomBlockRegistry.explosionDecayAmount(4, 4.0F, randomValues::removeFirst))
                .isEqualTo(2);
    }

    @Test
    void bindingRejectsUnregisteredCustomItem() {
        var registry = new FandCustomBlockRegistry(new NoopEventBus(), new FandCustomItemRegistry());
        registry.register(new CustomBlockType(MACHINE_ID, STONE));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.bindItem(Key.key("test:missing"), MACHINE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown custom block item");
    }

    @Test
    void registrationExposesLogicalTypeWhilePlacementUsesVanillaBase() {
        var registry = new FandCustomBlockRegistry(new NoopEventBus());
        var registration = registry.register(new CustomBlockType(MACHINE_ID, STONE));
        var block = new TestWorld().blockAt(1, 64, 1);

        assertThat(registration.type().key()).isEqualTo(MACHINE_ID);
        assertThat(registration.type().physics()).isEqualTo(STONE.physics());
        assertThat(registry.place(block, registration.type())).isTrue();
        assertThat(block.type()).isEqualTo(STONE);
        assertThat(registry.customBlock(block)).contains(registration.type());
    }

    @Test
    void placementAppliesConfiguredCarrierState() {
        var registry = new FandCustomBlockRegistry(new NoopEventBus());
        var registration = registry.register(CustomBlockType.builder(MACHINE_ID, STONE)
                .state("variant", "custom")
                .build());
        var block = (TestBlock) new TestWorld().blockAt(1, 64, 1);

        assertThat(registry.place(block, registration.type())).isTrue();
        assertThat(block.stateProperty("variant")).contains("custom");
    }

    @Test
    void placementAcceptsCarrierPropertyAlreadyAtRequestedValue() {
        var registry = new FandCustomBlockRegistry(new NoopEventBus());
        var registration = registry.register(CustomBlockType.builder(MACHINE_ID, STONE)
                .state("variant", "custom")
                .build());
        var block = (TestBlock) new TestWorld().blockAt(1, 64, 1);
        block.stateProperties.put("variant", "custom");

        assertThat(registry.place(block, registration.type())).isTrue();
        assertThat(block.stateProperty("variant")).contains("custom");
    }

    @Test
    void baseBehaviorIsIsolatedUnlessExplicitlyInherited() {
        var isolated = CustomBlockType.builder(MACHINE_ID, STONE).build();
        var inherited = CustomBlockType.builder(Key.key("test:inherited"), STONE)
                .inheritBaseBehavior(true)
                .build();

        assertThat(isolated.inheritBaseBehavior()).isFalse();
        assertThat(inherited.inheritBaseBehavior()).isTrue();
    }

    @Test
    void boundItemUsesPacketFaceInsteadOfPlayerLookDirection() {
        var events = new EventDispatcher();
        var items = new FandCustomItemRegistry();
        var item = items.register(CustomItemType.of(
                Key.key("test:machine_item"),
                new TestItemType(Key.key("minecraft:note_block")))).type();
        var registry = new FandCustomBlockRegistry(events, items);
        registry.register(new CustomBlockType(MACHINE_ID, STONE));
        registry.bindItem(item.id(), MACHINE_ID);
        assertThat(registry.isBoundItem(item.one())).isTrue();
        var world = new TestWorld();
        var clicked = (TestBlock) world.blockAt(1, 64, 1);
        var east = (TestBlock) clicked.relative(BlockFace.EAST);
        east.replaceable = true;
        var event = new PlayerMainHandRightClickBlockEvent(
                creativePlayer(),
                clicked,
                item.one(),
                BlockFace.EAST);

        events.fire(event);

        assertThat(event.cancelled()).isTrue();
        assertThat(registry.customBlock(east).map(CustomBlockType::id)).contains(MACHINE_ID);
    }

    @Test
    void boundItemNeverFallsBackToVanillaBaseWhenPlacementIsBlocked() {
        var events = new EventDispatcher();
        var items = new FandCustomItemRegistry();
        var item = items.register(CustomItemType.of(
                Key.key("test:machine_item"),
                new TestItemType(Key.key("minecraft:note_block")))).type();
        var registry = new FandCustomBlockRegistry(events, items);
        registry.register(new CustomBlockType(MACHINE_ID, STONE));
        registry.bindItem(item.id(), MACHINE_ID);
        var world = new TestWorld();
        var clicked = (TestBlock) world.blockAt(1, 64, 1);
        var east = clicked.relative(BlockFace.EAST);
        var event = new PlayerMainHandRightClickBlockEvent(
                creativePlayer(),
                clicked,
                item.one(),
                BlockFace.EAST);

        events.fire(event);

        assertThat(event.cancelled()).isTrue();
        assertThat(registry.customBlock(east)).isEmpty();
    }

    @Test
    void playerPlacementUsesVanillaEntityCollisionCheck() {
        var position = new BlockPos(1, 64, 1);
        var type = CustomBlockType.builder(MACHINE_ID, FandBlockType.of(Blocks.NOTE_BLOCK))
                .state("instrument", "custom_head")
                .build();
        var state = FandCustomBlockRegistry.applyCarrierState(type, Blocks.NOTE_BLOCK.defaultBlockState());
        var level = new TestCollisionGetter(state, false);

        assertThat(FandCustomBlockRegistry.hasPlacementClearance(level, state, position, null)).isFalse();
        assertThat(level.checkedShape).isNotNull().matches(shape -> !shape.isEmpty());

        level.unobstructed = true;
        assertThat(FandCustomBlockRegistry.hasPlacementClearance(level, state, position, null)).isTrue();
    }

    private static Player creativePlayer() {
        return (Player) java.lang.reflect.Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "gameMode" -> GameMode.CREATIVE;
                    case "toString" -> "creative player";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> args != null && args.length == 1 && proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
    }

    private record TestBlockType(Key key) implements BlockType {
    }

    private record TestItemType(Key key) implements ItemType {

        @Override
        public int maxStackSize() {
            return 64;
        }
    }

    private static final class TestWorld implements World {

        private final Map<String, TestBlock> blocks = new java.util.concurrent.ConcurrentHashMap<>();
        private final Key key;

        private TestWorld() {
            this(Key.key("minecraft:overworld"));
        }

        private TestWorld(Key key) {
            this.key = key;
        }

        @Override
        public Key key() {
            return key;
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
            return blocks.values().stream().filter(block -> block.components().contains(key)).toList();
        }

        @Override
        public Collection<? extends Block> blocksWith(DataComponentKey<?> key, int chunkX, int chunkZ) {
            return blocks.values().stream()
                    .filter(block -> (block.x() >> 4) == chunkX && (block.z() >> 4) == chunkZ)
                    .filter(block -> block.components().contains(key))
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
        private final Map<String, String> stateProperties = new java.util.concurrent.ConcurrentHashMap<>();
        private BlockType type = STONE;
        private boolean replaceable;

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
        public boolean replaceable() {
            return replaceable;
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
        public Map<String, String> stateProperties() {
            return Map.copyOf(stateProperties);
        }

        @Override
        public boolean setStateProperty(String name, String value) {
            stateProperties.put(name, value);
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

    private static final class TestCollisionGetter implements CollisionGetter {

        private final BlockState state;
        private boolean unobstructed;
        private @Nullable VoxelShape checkedShape;

        private TestCollisionGetter(BlockState state, boolean unobstructed) {
            this.state = state;
            this.unobstructed = unobstructed;
        }

        @Override
        public boolean isUnobstructed(@Nullable Entity source, VoxelShape shape) {
            checkedShape = shape;
            return unobstructed;
        }

        @Override
        public net.minecraft.world.level.border.WorldBorder getWorldBorder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
            return null;
        }

        @Override
        public List<VoxelShape> getEntityCollisions(@Nullable Entity source, AABB testArea) {
            return List.of();
        }

        @Override
        public net.minecraft.world.level.block.entity.@Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return state;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinY() {
            return -64;
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
