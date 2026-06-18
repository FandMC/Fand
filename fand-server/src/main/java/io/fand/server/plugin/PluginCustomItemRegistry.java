package io.fand.server.plugin;

import io.fand.api.customitem.CustomItemRegistration;
import io.fand.api.customitem.CustomItemRegistry;
import io.fand.api.customitem.CustomItemType;
import io.fand.api.item.ItemStack;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public final class PluginCustomItemRegistry implements CustomItemRegistry {

    private final CustomItemRegistry delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginCustomItemRegistry(CustomItemRegistry delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public CustomItemRegistration register(CustomItemType type) {
        Objects.requireNonNull(type, "type");
        return tracker.track(delegate.register(new CustomItemType(
                scopedKey(type.id()),
                type.baseType(),
                type.template())));
    }

    @Override
    public Optional<CustomItemType> type(Key id) {
        return delegate.type(scopedKey(id)).filter(this::ownedByThisPlugin);
    }

    @Override
    public Collection<CustomItemType> types() {
        return delegate.types().stream().filter(this::ownedByThisPlugin).toList();
    }

    @Override
    public Optional<CustomItemType> customItem(ItemStack stack) {
        return delegate.customItem(stack).filter(this::ownedByThisPlugin);
    }

    @Override
    public Optional<Key> customId(ItemStack stack) {
        return customItem(stack).map(CustomItemType::id);
    }

    @Override
    public ItemStack create(Key id, int amount) {
        return delegate.create(scopedKey(id), amount);
    }

    @Override
    public ItemStack tag(ItemStack stack, Key id) {
        return delegate.tag(stack, scopedKey(id));
    }

    @Override
    public ItemStack untag(ItemStack stack) {
        if (customId(stack).isEmpty()) {
            return stack;
        }
        return delegate.untag(stack);
    }

    private Key scopedKey(Key key) {
        Objects.requireNonNull(key, "key");
        if (namespace.equals(key.namespace())) {
            return key;
        }
        return Key.key(namespace, key.value());
    }

    private boolean ownedByThisPlugin(CustomItemType type) {
        return namespace.equals(type.id().namespace());
    }
}
