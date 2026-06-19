package io.fand.server.plugin;

import io.fand.api.enchantment.CustomEnchantment;
import io.fand.api.enchantment.EnchantmentRegistry;
import io.fand.api.enchantment.EnchantmentRegistration;
import io.fand.api.enchantment.EnchantmentView;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public final class PluginEnchantmentRegistry implements EnchantmentRegistry {

    private final EnchantmentRegistry delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginEnchantmentRegistry(EnchantmentRegistry delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public Optional<EnchantmentView> enchantment(Key key) {
        return delegate.enchantment(scopedKey(key)).filter(this::ownedByThisPlugin);
    }

    @Override
    public EnchantmentRegistration register(CustomEnchantment enchantment) {
        Objects.requireNonNull(enchantment, "enchantment");
        return tracker.track(delegate.register(new CustomEnchantment(
                scopedKey(enchantment.key()),
                enchantment.description(),
                enchantment.definition(),
                enchantment.effects(),
                enchantment.exclusiveSet())));
    }

    private Key scopedKey(Key key) {
        Objects.requireNonNull(key, "key");
        if (namespace.equals(key.namespace())) {
            return key;
        }
        return Key.key(namespace, key.value());
    }

    private boolean ownedByThisPlugin(EnchantmentView view) {
        return namespace.equals(view.key().namespace());
    }
}
