package io.fand.server.plugin;

import io.fand.api.item.custom.CustomItemRegistration;
import io.fand.api.item.custom.CustomItemRegistry;
import io.fand.api.item.custom.CustomItemType;
import io.fand.api.item.ItemStack;
import io.fand.api.item.component.ItemComponentKeys;
import io.fand.api.item.component.ItemKeySet;
import io.fand.api.item.component.ItemTool;
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
        var scopedId = scopedKey(type.id());
        var components = type.template().itemModel().filter(type.id()::equals).isPresent()
                ? type.defaultComponents().withKey(ItemComponentKeys.ITEM_MODEL, scopedId)
                : type.defaultComponents();
        return tracker.track(delegate.register(new CustomItemType(
                scopedId,
                type.baseType(),
                components,
                type.customBlockToolRules().stream().map(this::scopedRule).toList())));
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

    private ItemTool.Rule scopedRule(ItemTool.Rule rule) {
        var blocks = rule.blocks();
        ItemKeySet scopedBlocks = blocks.tag()
                .map(tag -> ItemKeySet.tag(scopedKey(tag)))
                .orElseGet(() -> ItemKeySet.of(blocks.values().stream().map(this::scopedKey).toList()));
        return new ItemTool.Rule(
                scopedBlocks,
                rule.speed().orElse(null),
                rule.correctForDrops().orElse(null));
    }
}
