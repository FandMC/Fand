package io.fand.server.block;

import io.fand.api.block.Block;
import io.fand.api.block.component.BlockComponentKeys;
import io.fand.api.component.DataComponentMap;
import io.fand.api.customblock.CustomBlockContext;
import io.fand.api.customblock.CustomBlockItemBinding;
import io.fand.api.customblock.CustomBlockListener;
import io.fand.api.customblock.CustomBlockRegistration;
import io.fand.api.customblock.CustomBlockRegistry;
import io.fand.api.customblock.CustomBlockType;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.event.block.BlockBreakEvent;
import io.fand.api.event.block.BlockPlaceEvent;
import io.fand.api.event.player.PlayerInteractEvent;
import io.fand.api.item.ItemStack;
import io.fand.api.world.BlockRayTraceResult;
import io.fand.api.world.Location;
import io.fand.api.world.Vector3;
import io.fand.api.world.World;
import io.fand.server.item.FandCustomItemRegistry;
import io.fand.server.component.BlockComponentStorage;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.key.Key;

public final class FandCustomBlockRegistry implements CustomBlockRegistry {

    private static final CustomBlockListener NOOP_LISTENER = new CustomBlockListener() {
    };

    private final ConcurrentHashMap<Key, RegisteredCustomBlock> types = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Key, RegisteredItemBinding> itemBindings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ChunkKey, Set<BlockPosKey>> activeTickingBlocks = new ConcurrentHashMap<>();
    private final EventBus events;
    private final FandCustomItemRegistry customItems;
    private final AtomicReference<LifecycleSubscriptions> lifecycleSubscriptions = new AtomicReference<>();

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
        return block.components().get(BlockComponentKeys.CUSTOM_ID);
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
        var active = new HashSet<BlockPosKey>();
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
            activeTickingBlocks.remove(chunkKey);
        } else {
            activeTickingBlocks.put(chunkKey, Collections.synchronizedSet(active));
        }
    }

    public void handleChunkUnloaded(World world, int chunkX, int chunkZ) {
        if (types.isEmpty()) {
            return;
        }
        activeTickingBlocks.remove(new ChunkKey(world.key(), chunkX, chunkZ));
        for (var block : customBlocks(world, chunkX, chunkZ)) {
            customId(block).flatMap(this::activeRegistration).ifPresent(registered -> fireUnloaded(registered, block));
        }
    }

    public void tick(World world) {
        if (types.values().stream().noneMatch(registration -> registration.active() && registration.type().ticking())) {
            return;
        }
        for (var entry : activeTickingBlocks.entrySet()) {
            var chunk = entry.getKey();
            if (!chunk.world().equals(world.key()) || !world.chunkLoaded(chunk.chunkX(), chunk.chunkZ())) {
                continue;
            }
            var positions = entry.getValue().toArray(BlockPosKey[]::new);
            for (var position : positions) {
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
        if (types.isEmpty()) {
            closeLifecycleSubscriptions();
            activeTickingBlocks.clear();
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
            activeTickingBlocks
                    .computeIfAbsent(chunkKey, ignored -> Collections.synchronizedSet(new HashSet<>()))
                    .add(position);
        } else {
            removeActiveTickingBlock(block);
        }
    }

    private void removeActiveTickingBlock(Block block) {
        var positions = activeTickingBlocks.get(new ChunkKey(block.world().key(), block.x() >> 4, block.z() >> 4));
        if (positions != null) {
            positions.remove(BlockPosKey.of(block));
        }
    }

    private void ensureLifecycleSubscriptions() {
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
        if (!lifecycleSubscriptions.compareAndSet(null, created)) {
            created.close();
        }
    }

    private void closeLifecycleSubscriptions() {
        var subscriptions = lifecycleSubscriptions.getAndSet(null);
        if (subscriptions != null) {
            subscriptions.close();
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
        if (item.isEmpty() || player.gameMode() == io.fand.api.entity.GameMode.CREATIVE) {
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
        public Key id() {
            return type.id();
        }

        private CustomBlockType type() {
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
