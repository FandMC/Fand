package io.fand.server.block;

import io.fand.api.block.Block;
import io.fand.api.block.component.BlockComponentKeys;
import io.fand.api.component.DataComponentMap;
import io.fand.api.block.custom.CustomBlockContext;
import io.fand.api.block.custom.CustomBlockItemBinding;
import io.fand.api.block.custom.CustomBlockListener;
import io.fand.api.block.custom.CustomBlockRegistration;
import io.fand.api.block.custom.CustomBlockRegistry;
import io.fand.api.block.custom.CustomBlockType;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.event.block.BlockBreakEvent;
import io.fand.api.event.block.BlockPlaceEvent;
import io.fand.api.event.player.PlayerInteractEvent;
import io.fand.api.item.ItemStack;
import io.fand.api.item.custom.CustomItemType;
import io.fand.api.world.BlockRayTraceResult;
import io.fand.api.world.Location;
import io.fand.api.world.Vector3;
import io.fand.api.world.World;
import io.fand.server.item.FandCustomItemRegistry;
import io.fand.server.component.BlockComponentStorage;
import io.fand.server.item.FandItemStacks;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public final class FandCustomBlockRegistry implements CustomBlockRegistry {

    private static final CustomBlockListener NOOP_LISTENER = new CustomBlockListener() {
    };

    private final ConcurrentHashMap<Key, RegisteredCustomBlock> types = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Key, RegisteredItemBinding> itemBindings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Key, ConcurrentHashMap<ChunkKey, Set<BlockPosKey>>> activeTickingBlocksByWorld =
            new ConcurrentHashMap<>();
    private final EventBus events;
    private final FandCustomItemRegistry customItems;
    private final AtomicReference<LifecycleSubscriptions> lifecycleSubscriptions = new AtomicReference<>();

    // Serializes unregister's teardown (close subscriptions once types becomes
    // empty) against ensure's lazy creation. Without this, a register that
    // interleaves the teardown can observe the about-to-be-closed subscriptions,
    // skip creating its own, and lose its event handlers.
    private final Object lifecycleLock = new Object();

    public FandCustomBlockRegistry(EventBus events) {
        this(events, null);
    }

    public FandCustomBlockRegistry(EventBus events, FandCustomItemRegistry customItems) {
        this.events = Objects.requireNonNull(events, "events");
        this.customItems = customItems;
    }

    @Override
    public CustomBlockRegistration register(CustomBlockType type) {
        return register(type, NOOP_LISTENER);
    }

    @Override
    public CustomBlockRegistration register(CustomBlockType type, CustomBlockListener listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");
        validateCarrier(type);
        var registered = new RegisteredCustomBlock(this, type, listener);
        var previous = types.putIfAbsent(type.id(), registered);
        if (previous != null) {
            throw new IllegalArgumentException("Custom block already registered: " + type.id().asString());
        }
        ensureLifecycleSubscriptions();
        return registered;
    }

    @Override
    public Optional<CustomBlockType> type(Key id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(types.get(id)).filter(RegisteredCustomBlock::active).map(RegisteredCustomBlock::type);
    }

    @Override
    public Optional<CustomBlockType> customBlock(Block block) {
        return customId(block).flatMap(this::type);
    }

    @Override
    public Optional<CustomBlockType> blockForItem(Key itemId) {
        Objects.requireNonNull(itemId, "itemId");
        return Optional.ofNullable(itemBindings.get(itemId))
                .filter(RegisteredItemBinding::active)
                .flatMap(binding -> type(binding.blockId()));
    }

    public Optional<Key> customId(Block block) {
        Objects.requireNonNull(block, "block");
        return block.components().value(BlockComponentKeys.CUSTOM_ID);
    }

    @Override
    public Collection<CustomBlockType> types() {
        return types.values().stream()
                .filter(RegisteredCustomBlock::active)
                .map(RegisteredCustomBlock::type)
                .sorted(java.util.Comparator.comparing(type -> type.id().asString()))
                .toList();
    }

    @Override
    public CustomBlockItemBinding bindItem(Key itemId, Key blockId) {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(blockId, "blockId");
        registered(blockId);
        if (customItems != null && customItems.type(itemId).isEmpty()) {
            throw new IllegalArgumentException("Unknown custom block item: " + itemId.asString());
        }
        var binding = new RegisteredItemBinding(this, itemId, blockId);
        var previous = itemBindings.putIfAbsent(itemId, binding);
        if (previous != null) {
            throw new IllegalArgumentException("Custom block item binding already registered: " + itemId.asString());
        }
        return binding;
    }

    @Override
    public void unbindItem(Key itemId) {
        Objects.requireNonNull(itemId, "itemId");
        var binding = itemBindings.remove(itemId);
        if (binding != null) {
            binding.deactivate();
        }
    }

    @Override
    public boolean place(Block block, Key id) {
        return place(block, id, DataComponentMap.EMPTY);
    }

    @Override
    public boolean place(Block block, Key id, DataComponentMap components) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(components, "components");
        var registered = registered(id);
        var merged = registered.type().defaultComponents()
                .apply(components)
                .with(BlockComponentKeys.CUSTOM_ID, registered.type().id());
        if (registered.type().ticking()) {
            merged = merged.with(BlockComponentKeys.TICKING, true);
        } else {
            merged = merged.without(BlockComponentKeys.TICKING);
        }
        if (!block.setType(registered.type().baseType(), merged)) {
            return false;
        }
        for (var property : registered.type().baseStateProperties().entrySet()) {
            if (!block.stateProperty(property.getKey()).filter(property.getValue()::equals).isPresent()
                    && !block.setStateProperty(property.getKey(), property.getValue())) {
                throw new IllegalStateException("Registered carrier state is no longer valid: "
                        + property.getKey() + "=" + property.getValue());
            }
        }
        refreshActiveTickingBlock(block);
        firePlaced(registered, block);
        return true;
    }

    @Override
    public boolean remove(Block block) {
        Objects.requireNonNull(block, "block");
        var registered = customId(block).flatMap(this::activeRegistration).orElse(null);
        if (registered == null) {
            return false;
        }
        fireBroken(registered, block);
        block.components().clear();
        removeActiveTickingBlock(block);
        return true;
    }

    @Override
    public Collection<Block> customBlocks(World world, int chunkX, int chunkZ) {
        return blocksWith(world, BlockComponentKeys.CUSTOM_ID, chunkX, chunkZ);
    }

    @Override
    public Collection<Block> tickingBlocks(World world, int chunkX, int chunkZ) {
        return blocksWith(world, BlockComponentKeys.TICKING, chunkX, chunkZ).stream()
                .filter(block -> customId(block).flatMap(this::type).map(CustomBlockType::ticking).orElse(false))
                .toList();
    }

    public void handlePlaced(Block block) {
        customId(block).flatMap(this::activeRegistration).ifPresent(registered -> {
            refreshActiveTickingBlock(block);
            firePlaced(registered, block);
        });
    }

    public void handleBroken(Block block) {
        customId(block).flatMap(this::activeRegistration).ifPresent(registered -> {
            removeActiveTickingBlock(block);
            fireBroken(registered, block);
        });
    }

    public void handleChunkLoaded(World world, int chunkX, int chunkZ) {
        if (types.isEmpty()) {
            return;
        }
        var active = ConcurrentHashMap.<BlockPosKey>newKeySet();
        for (var block : customBlocks(world, chunkX, chunkZ)) {
            customId(block).flatMap(this::activeRegistration).ifPresent(registered -> {
                if (registered.type().ticking()) {
                    active.add(BlockPosKey.of(block));
                }
                fireLoaded(registered, block);
            });
        }
        var chunkKey = new ChunkKey(world.key(), chunkX, chunkZ);
        if (active.isEmpty()) {
            removeActiveTickingChunk(chunkKey);
        } else {
            activeTickingBlocksByWorld
                    .computeIfAbsent(world.key(), ignored -> new ConcurrentHashMap<>())
                    .put(chunkKey, active);
        }
    }

    public void handleChunkUnloaded(World world, int chunkX, int chunkZ) {
        if (types.isEmpty()) {
            return;
        }
        removeActiveTickingChunk(new ChunkKey(world.key(), chunkX, chunkZ));
        for (var block : customBlocks(world, chunkX, chunkZ)) {
            customId(block).flatMap(this::activeRegistration).ifPresent(registered -> fireUnloaded(registered, block));
        }
    }

    public void tick(World world) {
        if (types.values().stream().noneMatch(registration -> registration.active() && registration.type().ticking())) {
            return;
        }
        var activeTickingBlocks = activeTickingBlocksByWorld.get(world.key());
        if (activeTickingBlocks == null) {
            return;
        }
        for (var entry : activeTickingBlocks.entrySet()) {
            var chunk = entry.getKey();
            if (!world.chunkLoaded(chunk.chunkX(), chunk.chunkZ())) {
                continue;
            }
            for (var position : entry.getValue()) {
                var block = world.blockAt(position.x(), position.y(), position.z());
                var registered = customId(block).flatMap(this::activeRegistration).orElse(null);
                if (registered == null || !registered.type().ticking()) {
                    entry.getValue().remove(position);
                    continue;
                }
                fireTick(registered, block);
            }
        }
    }

    public Optional<MiningProperties> mining(ServerLevel level, BlockPos position, net.minecraft.world.item.ItemStack tool) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(tool, "tool");
        var blockType = customType(level, position).orElse(null);
        if (blockType == null) {
            return Optional.empty();
        }
        var mining = blockType.mining();
        var miningState = FandBlockType.unwrap(mining.toolRuleBase()).defaultBlockState();
        float toolSpeed = tool.getDestroySpeed(miningState);
        boolean correctForDrops = tool.isCorrectToolForDrops(miningState);
        if (customItems != null) {
            var customItem = customItems.customItem(FandItemStacks.fromVanilla(tool)).orElse(null);
            if (customItem != null) {
                var rule = customItem.customBlockToolRule(blockType).orElse(null);
                if (rule != null) {
                    toolSpeed = rule.speed().orElse(toolSpeed);
                    correctForDrops = rule.correctForDrops().orElse(correctForDrops);
                }
            }
        }
        if (!mining.requiresCorrectTool()) {
            correctForDrops = true;
        }
        return Optional.of(new MiningProperties(mining.hardness(), toolSpeed, correctForDrops));
    }

    public Optional<Float> hardness(ServerLevel level, BlockPos position) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        return customType(level, position).map(type -> type.mining().hardness());
    }

    public Optional<ItemStack> defaultDrop(Key blockId) {
        Objects.requireNonNull(blockId, "blockId");
        if (customItems == null || type(blockId).isEmpty()) {
            return Optional.empty();
        }
        return itemBindings.values().stream()
                .filter(RegisteredItemBinding::active)
                .filter(binding -> binding.blockId().equals(blockId))
                .sorted(java.util.Comparator
                        .comparing((RegisteredItemBinding binding) -> !binding.itemId().equals(blockId))
                        .thenComparing(binding -> binding.itemId().asString()))
                .map(RegisteredItemBinding::itemId)
                .map(customItems::type)
                .flatMap(Optional::stream)
                .findFirst()
                .map(CustomItemType::one);
    }

    public @Nullable List<net.minecraft.world.item.ItemStack> playerBreakDrops(
            ServerLevel level,
            BlockPos position
    ) {
        var blockType = customType(level, position).orElse(null);
        if (blockType == null) {
            return null;
        }
        return defaultDrop(blockType.id())
                .map(FandItemStacks::toVanilla)
                .map(java.util.List::of)
                .orElseGet(java.util.List::of);
    }

    public BlockState preserveCarrierState(ServerLevel level, BlockPos position, BlockState proposed) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(proposed, "proposed");
        var type = customType(level, position).orElse(null);
        if (type == null || !(type.baseType() instanceof FandBlockType baseType) || proposed.getBlock() != baseType.handle()) {
            return proposed;
        }
        return applyCarrierState(type, proposed);
    }

    public boolean suppressBaseBehavior(ServerLevel level, BlockPos position) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        return customType(level, position)
                .map(type -> !type.inheritBaseBehavior())
                .orElse(false);
    }

    private Optional<CustomBlockType> customType(ServerLevel level, BlockPos position) {
        return BlockComponentStorage.snapshot(level, position)
                .value(BlockComponentKeys.CUSTOM_ID)
                .flatMap(this::type);
    }

    private static void validateCarrier(CustomBlockType type) {
        if (!(type.baseType() instanceof FandBlockType baseType)) {
            return;
        }
        applyCarrierState(type, baseType.handle().defaultBlockState());
    }

    static BlockState applyCarrierState(CustomBlockType type, BlockState state) {
        for (var entry : type.baseStateProperties().entrySet()) {
            state = stateWithProperty(state, entry.getKey(), entry.getValue())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown carrier state " + entry.getKey()
                            + "=" + entry.getValue() + " for " + type.baseType().key().asString()));
        }
        return state;
    }

    private static Optional<BlockState> stateWithProperty(BlockState state, String name, String value) {
        for (var property : state.getProperties()) {
            if (property.getName().equals(name)) {
                return stateWithProperty(state, property, value);
            }
        }
        return Optional.empty();
    }

    private static <T extends Comparable<T>> Optional<BlockState> stateWithProperty(
            BlockState state,
            Property<T> property,
            String value
    ) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed));
    }

    private Collection<Block> blocksWith(World world, io.fand.api.component.DataComponentKey<?> key, int chunkX, int chunkZ) {
        Objects.requireNonNull(world, "world");
        return world.blocksWith(key, chunkX, chunkZ).stream()
                .map(Block.class::cast)
                .toList();
    }

    private RegisteredCustomBlock registered(Key id) {
        return activeRegistration(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown custom block: " + id.asString()));
    }

    private Optional<RegisteredCustomBlock> activeRegistration(Key id) {
        return Optional.ofNullable(types.get(id)).filter(RegisteredCustomBlock::active);
    }

    private void firePlaced(RegisteredCustomBlock registered, Block block) {
        registered.listener().placed(context(registered, block));
    }

    private void fireBroken(RegisteredCustomBlock registered, Block block) {
        registered.listener().broken(context(registered, block));
    }

    private void fireLoaded(RegisteredCustomBlock registered, Block block) {
        registered.listener().loaded(context(registered, block));
    }

    private void fireUnloaded(RegisteredCustomBlock registered, Block block) {
        registered.listener().unloaded(context(registered, block));
    }

    private void fireTick(RegisteredCustomBlock registered, Block block) {
        registered.listener().tick(context(registered, block));
    }

    private CustomBlockContext context(RegisteredCustomBlock registered, Block block) {
        return new CustomBlockContext(this, registered.type(), block);
    }

    private void unregister(RegisteredCustomBlock registration) {
        types.remove(registration.id(), registration);
        itemBindings.values().removeIf(binding -> {
            if (!binding.blockId().equals(registration.id())) {
                return false;
            }
            binding.deactivate();
            return true;
        });
        LifecycleSubscriptions toClose = null;
        synchronized (lifecycleLock) {
            if (types.isEmpty()) {
                toClose = lifecycleSubscriptions.getAndSet(null);
                activeTickingBlocksByWorld.clear();
            }
        }
        if (toClose != null) {
            toClose.close();
        }
    }

    private void unregister(RegisteredItemBinding binding) {
        itemBindings.remove(binding.itemId(), binding);
    }

    private void refreshActiveTickingBlock(Block block) {
        var chunkKey = new ChunkKey(block.world().key(), block.x() >> 4, block.z() >> 4);
        var position = BlockPosKey.of(block);
        var registered = customId(block).flatMap(this::activeRegistration).orElse(null);
        if (registered != null && registered.type().ticking() && block.world().chunkLoaded(chunkKey.chunkX(), chunkKey.chunkZ())) {
            activeTickingBlocksByWorld
                    .computeIfAbsent(chunkKey.world(), ignored -> new ConcurrentHashMap<>())
                    .computeIfAbsent(chunkKey, ignored -> ConcurrentHashMap.newKeySet())
                    .add(position);
        } else {
            removeActiveTickingBlock(block);
        }
    }

    private void removeActiveTickingBlock(Block block) {
        var positions = activeTickingBlocks(block.world().key())
                .map(chunks -> chunks.get(new ChunkKey(block.world().key(), block.x() >> 4, block.z() >> 4)))
                .orElse(null);
        if (positions != null) {
            positions.remove(BlockPosKey.of(block));
        }
    }

    private Optional<ConcurrentHashMap<ChunkKey, Set<BlockPosKey>>> activeTickingBlocks(Key world) {
        return Optional.ofNullable(activeTickingBlocksByWorld.get(world));
    }

    private void removeActiveTickingChunk(ChunkKey chunkKey) {
        var chunks = activeTickingBlocksByWorld.get(chunkKey.world());
        if (chunks == null) {
            return;
        }
        chunks.remove(chunkKey);
        if (chunks.isEmpty()) {
            activeTickingBlocksByWorld.remove(chunkKey.world(), chunks);
        }
    }

    private void ensureLifecycleSubscriptions() {
        synchronized (lifecycleLock) {
            if (lifecycleSubscriptions.get() != null) {
                return;
            }
            var created = new LifecycleSubscriptions(
                    events.subscribe(BlockPlaceEvent.class, EventPriority.OBSERVER, event -> handlePlaced(event.block())),
                    events.subscribe(BlockBreakEvent.class, EventPriority.OBSERVER, event -> {
                        if (!event.cancelled()) {
                            handleBroken(event.block());
                        }
                    }),
                    events.subscribe(PlayerInteractEvent.class, EventPriority.HIGHEST, this::handleInteract));
            lifecycleSubscriptions.set(created);
        }
    }

    private boolean handleInteract(PlayerInteractEvent event) {
        if (event.cancelled() || event.action() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || customItems == null) {
            return true;
        }
        var itemId = customItems.customId(event.item()).orElse(null);
        if (itemId == null) {
            return true;
        }
        var binding = itemBindings.get(itemId);
        if (binding == null || !binding.active()) {
            return true;
        }
        var blockType = type(binding.blockId()).orElse(null);
        if (blockType == null) {
            return true;
        }
        var target = event.block()
                .flatMap(block -> placementBlock(event.player(), block))
                .orElse(null);
        if (target == null || !target.air()) {
            return true;
        }
        if (!place(target, blockType.id())) {
            return true;
        }
        consumePlacedItem(event.player(), event.hand(), event.item());
        event.setCancelled(true);
        return false;
    }

    private Optional<Block> placementBlock(io.fand.api.entity.Player player, Block clicked) {
        var location = player.location().offset(0.0, 1.62, 0.0);
        var direction = lookDirection(location);
        return clicked.world()
                .rayTraceBlock(location, direction, 6.0)
                .filter(hit -> sameBlock(hit.block(), clicked))
                .map(hit -> adjacent(clicked, hit));
    }

    private static boolean sameBlock(Block left, Block right) {
        return left.x() == right.x()
                && left.y() == right.y()
                && left.z() == right.z()
                && left.world().key().equals(right.world().key());
    }

    private static Block adjacent(Block clicked, BlockRayTraceResult hit) {
        return switch (hit.face()) {
            case DOWN -> clicked.world().blockAt(clicked.x(), clicked.y() - 1, clicked.z());
            case UP -> clicked.world().blockAt(clicked.x(), clicked.y() + 1, clicked.z());
            case NORTH -> clicked.world().blockAt(clicked.x(), clicked.y(), clicked.z() - 1);
            case SOUTH -> clicked.world().blockAt(clicked.x(), clicked.y(), clicked.z() + 1);
            case WEST -> clicked.world().blockAt(clicked.x() - 1, clicked.y(), clicked.z());
            case EAST -> clicked.world().blockAt(clicked.x() + 1, clicked.y(), clicked.z());
        };
    }

    private static Vector3 lookDirection(Location location) {
        double yaw = Math.toRadians(location.yaw());
        double pitch = Math.toRadians(location.pitch());
        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);
        return new Vector3(x, y, z);
    }

    private static void consumePlacedItem(
            io.fand.api.entity.Player player,
            PlayerInteractEvent.Hand hand,
            ItemStack item
    ) {
        if (item.empty() || player.gameMode() == io.fand.api.entity.GameMode.CREATIVE) {
            return;
        }
        var next = item.amount() <= 1 ? ItemStack.EMPTY : item.withAmount(item.amount() - 1);
        if (hand == PlayerInteractEvent.Hand.OFF_HAND) {
            player.inventory().setOffhandItem(next);
        } else {
            player.inventory().setHeldItem(next);
        }
    }

    private record LifecycleSubscriptions(EventSubscription place, EventSubscription breakBlock, EventSubscription interact) implements AutoCloseable {

        @Override
        public void close() {
            place.unregister();
            breakBlock.unregister();
            interact.unregister();
        }
    }

    public record MiningProperties(float hardness, float toolSpeed, boolean correctForDrops) {
    }

    private static final class RegisteredCustomBlock implements CustomBlockRegistration {

        private final FandCustomBlockRegistry owner;
        private final CustomBlockType type;
        private final CustomBlockListener listener;
        private volatile boolean active = true;

        private RegisteredCustomBlock(FandCustomBlockRegistry owner, CustomBlockType type, CustomBlockListener listener) {
            this.owner = owner;
            this.type = type;
            this.listener = listener;
        }

        @Override
        public CustomBlockType type() {
            return type;
        }

        private CustomBlockListener listener() {
            return listener;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                owner.unregister(this);
            }
        }

        @Override
        public CustomBlockItemBinding bindItem(Key itemId) {
            return owner.bindItem(itemId, id());
        }

        @Override
        public void unbindItem(Key itemId) {
            owner.unbindItem(itemId);
        }
    }

    private static final class RegisteredItemBinding implements CustomBlockItemBinding {

        private final FandCustomBlockRegistry owner;
        private final Key itemId;
        private final Key blockId;
        private volatile boolean active = true;

        private RegisteredItemBinding(FandCustomBlockRegistry owner, Key itemId, Key blockId) {
            this.owner = owner;
            this.itemId = itemId;
            this.blockId = blockId;
        }

        @Override
        public Key itemId() {
            return itemId;
        }

        @Override
        public Key blockId() {
            return blockId;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                owner.unregister(this);
            }
        }

        private void deactivate() {
            active = false;
        }
    }

    private record ChunkKey(Key world, int chunkX, int chunkZ) {
    }

    private record BlockPosKey(int x, int y, int z) {

        private static BlockPosKey of(Block block) {
            return new BlockPosKey(block.x(), block.y(), block.z());
        }
    }
}
