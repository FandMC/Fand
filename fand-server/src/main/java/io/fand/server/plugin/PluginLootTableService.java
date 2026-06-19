package io.fand.server.plugin;

import io.fand.api.loot.LootContext;
import io.fand.api.loot.LootGenerator;
import io.fand.api.loot.LootTableRegistration;
import io.fand.api.loot.LootTableService;
import io.fand.api.loot.LootTableView;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public final class PluginLootTableService implements LootTableService {

    private final LootTableService delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginLootTableService(LootTableService delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public Optional<LootTableView> table(Key key) {
        return delegate.table(scopedKey(key)).filter(this::ownedByThisPlugin);
    }

    @Override
    public List<io.fand.api.item.ItemStack> generate(Key key, LootContext context) {
        return delegate.generate(scopedKey(key), context);
    }

    @Override
    public LootTableRegistration replace(Key key, LootGenerator generator) {
        return tracker.track(delegate.replace(scopedKey(key), generator));
    }

    private Key scopedKey(Key key) {
        Objects.requireNonNull(key, "key");
        if (namespace.equals(key.namespace())) {
            return key;
        }
        return Key.key(namespace, key.value());
    }

    private boolean ownedByThisPlugin(LootTableView view) {
        return namespace.equals(view.key().namespace());
    }
}
