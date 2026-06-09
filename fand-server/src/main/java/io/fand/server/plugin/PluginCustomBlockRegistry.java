package io.fand.server.plugin;

import io.fand.api.block.Block;
import io.fand.api.component.DataComponentMap;
import io.fand.api.customblock.CustomBlockItemBinding;
import io.fand.api.customblock.CustomBlockListener;
import io.fand.api.customblock.CustomBlockRegistration;
import io.fand.api.customblock.CustomBlockRegistry;
import io.fand.api.customblock.CustomBlockType;
import io.fand.api.world.World;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public final class PluginCustomBlockRegistry implements CustomBlockRegistry {

    private final CustomBlockRegistry delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginCustomBlockRegistry(CustomBlockRegistry delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public CustomBlockRegistration register(CustomBlockType type) {
        return new ScopedCustomBlockRegistration(tracker.track(delegate.register(scoped(type))));
    }

    @Override
    public CustomBlockRegistration register(CustomBlockType type, CustomBlockListener listener) {
        return new ScopedCustomBlockRegistration(tracker.track(delegate.register(scoped(type), listener)));
    }

    @Override
    public Optional<CustomBlockType> type(Key id) {
        return delegate.type(scopedKey(id)).filter(this::ownedByThisPlugin);
    }

    @Override
    public Optional<CustomBlockType> customBlock(Block block) {
        return delegate.customBlock(block).filter(this::ownedByThisPlugin);
    }

    @Override
    public Optional<CustomBlockType> blockForItem(Key itemId) {
        return delegate.blockForItem(scopedKey(itemId)).filter(this::ownedByThisPlugin);
    }

    @Override
    public Collection<CustomBlockType> types() {
        return delegate.types().stream().filter(this::ownedByThisPlugin).toList();
    }

    @Override
    public CustomBlockItemBinding bindItem(Key itemId, Key blockId) {
        return tracker.track(delegate.bindItem(scopedKey(itemId), scopedKey(blockId)));
    }

    @Override
    public void unbindItem(Key itemId) {
        delegate.unbindItem(scopedKey(itemId));
    }

    @Override
    public boolean place(Block block, Key id) {
        return delegate.place(block, scopedKey(id));
    }

    @Override
    public boolean place(Block block, Key id, DataComponentMap components) {
        return delegate.place(block, scopedKey(id), components);
    }

    @Override
    public boolean remove(Block block) {
        return customBlock(block).isPresent() && delegate.remove(block);
    }

    @Override
    public Collection<Block> customBlocks(World world, int chunkX, int chunkZ) {
        return delegate.customBlocks(world, chunkX, chunkZ).stream()
                .filter(block -> customBlock(block).isPresent())
                .toList();
    }

    @Override
    public Collection<Block> tickingBlocks(World world, int chunkX, int chunkZ) {
        return delegate.tickingBlocks(world, chunkX, chunkZ).stream()
                .filter(block -> customBlock(block).isPresent())
                .toList();
    }

    private CustomBlockType scoped(CustomBlockType type) {
        Objects.requireNonNull(type, "type");
        return new CustomBlockType(
                scopedKey(type.id()),
                type.baseType(),
                type.defaultComponents(),
                type.ticking());
    }

    private Key scopedKey(Key key) {
        Objects.requireNonNull(key, "key");
        if (namespace.equals(key.namespace())) {
            return key;
        }
        return Key.key(namespace, key.value());
    }

    private boolean ownedByThisPlugin(CustomBlockType type) {
        return namespace.equals(type.id().namespace());
    }

    private final class ScopedCustomBlockRegistration implements CustomBlockRegistration {

        private final CustomBlockRegistration delegateRegistration;

        private ScopedCustomBlockRegistration(CustomBlockRegistration delegateRegistration) {
            this.delegateRegistration = delegateRegistration;
        }

        @Override
        public Key id() {
            return delegateRegistration.id();
        }

        @Override
        public boolean active() {
            return delegateRegistration.active();
        }

        @Override
        public void unregister() {
            delegateRegistration.unregister();
        }

        @Override
        public CustomBlockItemBinding bindItem(Key itemId) {
            return PluginCustomBlockRegistry.this.bindItem(itemId, id());
        }

        @Override
        public void unbindItem(Key itemId) {
            PluginCustomBlockRegistry.this.unbindItem(itemId);
        }
    }
}
