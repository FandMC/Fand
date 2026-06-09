package io.fand.server.block;

import io.fand.api.block.Block;
import io.fand.api.block.component.BlockComponentKeys;
import io.fand.api.component.DataComponentMap;
import io.fand.api.customblock.CustomBlockContext;
import io.fand.api.customblock.CustomBlockListener;
import io.fand.api.customblock.CustomBlockRegistration;
import io.fand.api.customblock.CustomBlockRegistry;
import io.fand.api.customblock.CustomBlockType;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.event.block.BlockBreakEvent;
import io.fand.api.event.block.BlockPlaceEvent;
import io.fand.api.world.World;
import io.fand.server.component.BlockComponentStorage;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.key.Key;

public final class FandCustomBlockRegistry implements CustomBlockRegistry {

    private static final CustomBlockListener NOOP_LISTENER = new CustomBlockListener() {
    };

    private final ConcurrentHashMap<Key, RegisteredCustomBlock> types = new ConcurrentHashMap<>();
    private final EventBus events;
    private final AtomicReference<LifecycleSubscriptions> lifecycleSubscriptions = new AtomicReference<>();

    public FandCustomBlockRegistry(EventBus events) {
        this.events = Objects.requireNonNull(events, "events");
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
        customId(block).flatMap(this::activeRegistration).ifPresent(registered -> firePlaced(registered, block));
    }

    public void handleBroken(Block block) {
        customId(block).flatMap(this::activeRegistration).ifPresent(registered -> fireBroken(registered, block));
    }

    public void handleChunkLoaded(World world, int chunkX, int chunkZ) {
        if (types.isEmpty()) {
            return;
        }
        for (var block : customBlocks(world, chunkX, chunkZ)) {
            customId(block).flatMap(this::activeRegistration).ifPresent(registered -> fireLoaded(registered, block));
        }
    }

    public void handleChunkUnloaded(World world, int chunkX, int chunkZ) {
        if (types.isEmpty()) {
            return;
        }
        for (var block : customBlocks(world, chunkX, chunkZ)) {
            customId(block).flatMap(this::activeRegistration).ifPresent(registered -> fireUnloaded(registered, block));
        }
    }

    public void tick(World world) {
        if (types.values().stream().noneMatch(registration -> registration.active() && registration.type().ticking())) {
            return;
        }
        for (var block : world.blocksWith(BlockComponentKeys.TICKING)) {
            if (block.world().chunkLoaded(block.x() >> 4, block.z() >> 4)) {
                customId(block).flatMap(this::activeRegistration).ifPresent(registered -> {
                    if (registered.type().ticking()) {
                        fireTick(registered, block);
                    }
                });
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
        if (types.isEmpty()) {
            closeLifecycleSubscriptions();
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
                }));
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

    private record LifecycleSubscriptions(EventSubscription place, EventSubscription breakBlock) implements AutoCloseable {

        @Override
        public void close() {
            place.unregister();
            breakBlock.unregister();
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
    }
}
