package io.fand.server.enchantment;

import io.fand.api.enchantment.CustomEnchantment;
import io.fand.api.enchantment.EnchantmentRegistry;
import io.fand.api.enchantment.EnchantmentRegistration;
import io.fand.api.enchantment.EnchantmentView;
import io.fand.server.command.AdventureBridge;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.enchantment.Enchantment;

public final class FandEnchantmentRegistry implements EnchantmentRegistry {

    private final Supplier<MinecraftServer> server;
    private final Object lock = new Object();
    private final LinkedHashMap<Key, CustomEntry> customEnchantments = new LinkedHashMap<>();
    private final Set<Key> removedEnchantments = new HashSet<>();
    private final AtomicLong sequence = new AtomicLong();

    public FandEnchantmentRegistry(Supplier<MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public Optional<EnchantmentView> enchantment(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            var custom = customEnchantments.get(key);
            if (custom != null) {
                return Optional.of(custom.view());
            }
            if (removedEnchantments.contains(key)) {
                return Optional.empty();
            }
        }
        var current = server.get();
        if (current == null) {
            return Optional.empty();
        }
        return callOnServerThread(current, () -> current.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ResourceKey.create(Registries.ENCHANTMENT, identifier(key)))
                .map(holder -> {
                    var enchantment = holder.value();
                    return new EnchantmentView(
                            key,
                            AdventureBridge.fromVanilla(enchantment.description(), current.registryAccess()),
                            enchantment.getMaxLevel());
                }));
    }

    @Override
    public EnchantmentRegistration register(CustomEnchantment enchantment) {
        Objects.requireNonNull(enchantment, "enchantment");
        var token = sequence.incrementAndGet();
        var view = new EnchantmentView(enchantment.key(), enchantment.description(), enchantment.maxLevel());
        synchronized (lock) {
            removedEnchantments.remove(enchantment.key());
            customEnchantments.put(enchantment.key(), new CustomEntry(token, view, enchantment));
        }
        applyToVanilla(enchantment);
        return new Registration(this, enchantment.key(), token);
    }

    public void applyLoadedEnchantments() {
        var current = server.get();
        if (current == null) {
            return;
        }
        var snapshot = customEnchantments();
        if (snapshot.isEmpty()) {
            return;
        }
        snapshot.forEach(enchantment -> registerVanilla(current, enchantment));
    }

    public boolean registered(Key key) {
        synchronized (lock) {
            return customEnchantments.containsKey(key);
        }
    }

    public boolean remove(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            boolean removed = customEnchantments.remove(key) != null;
            if (removed) {
                removedEnchantments.add(key);
            }
            return removed;
        }
    }

    private boolean registered(Key key, long token) {
        synchronized (lock) {
            var entry = customEnchantments.get(key);
            return entry != null && entry.token() == token;
        }
    }

    private boolean remove(Key key, long token) {
        synchronized (lock) {
            var entry = customEnchantments.get(key);
            if (entry == null || entry.token() != token) {
                return false;
            }
            customEnchantments.remove(key);
            removedEnchantments.add(key);
            return true;
        }
    }

    private List<CustomEnchantment> customEnchantments() {
        synchronized (lock) {
            return customEnchantments.values().stream()
                    .map(CustomEntry::enchantment)
                    .toList();
        }
    }

    private void applyToVanilla(CustomEnchantment enchantment) {
        var current = server.get();
        if (current == null) {
            return;
        }
        callOnServerThread(current, () -> {
            registerVanilla(current, enchantment);
            return null;
        });
    }

    private static void registerVanilla(MinecraftServer server, CustomEnchantment custom) {
        var key = ResourceKey.create(Registries.ENCHANTMENT, identifier(custom.key()));
        var registry = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (!(registry instanceof MappedRegistry<Enchantment> mapped)) {
            throw new IllegalStateException("Enchantment registry is not writable: " + registry);
        }
        mapped.fand$registerRuntime(key, vanillaEnchantment(server, custom), RegistrationInfo.BUILT_IN);
    }

    private static Enchantment vanillaEnchantment(MinecraftServer server, CustomEnchantment custom) {
        var supportedItems = allItems(server);
        return new Enchantment(
                AdventureBridge.toVanilla(custom.description(), server.registryAccess()),
                Enchantment.definition(
                        supportedItems,
                        1,
                        custom.maxLevel(),
                        Enchantment.constantCost(1),
                        Enchantment.constantCost(1),
                        0,
                        EquipmentSlotGroup.ANY),
                HolderSet.empty(),
                net.minecraft.core.component.DataComponentMap.EMPTY);
    }

    private static HolderSet<Item> allItems(MinecraftServer server) {
        var items = server.registryAccess().lookupOrThrow(Registries.ITEM);
        List<Holder<Item>> holders = items.listElements()
                .filter(holder -> holder.value() != Items.AIR)
                .map(holder -> (Holder<Item>) holder)
                .toList();
        return HolderSet.direct(holders);
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static <T> T callOnServerThread(MinecraftServer server, Supplier<T> task) {
        if (server.isSameThread()) {
            return task.get();
        }
        return server.submit(task::get).join();
    }

    private record CustomEntry(long token, EnchantmentView view, CustomEnchantment enchantment) {
    }

    private static final class Registration implements EnchantmentRegistration {

        private final FandEnchantmentRegistry owner;
        private final Key key;
        private final long token;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private Registration(FandEnchantmentRegistry owner, Key key, long token) {
            this.owner = owner;
            this.key = key;
            this.token = token;
        }

        @Override
        public Key key() {
            return key;
        }

        @Override
        public boolean active() {
            return active.get() && owner.registered(key, token);
        }

        @Override
        public void close() {
            if (active.compareAndSet(true, false)) {
                owner.remove(key, token);
            }
        }
    }
}
