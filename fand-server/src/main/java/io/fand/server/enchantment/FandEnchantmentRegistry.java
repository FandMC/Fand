package io.fand.server.enchantment;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.fand.api.enchantment.CustomEnchantment;
import io.fand.api.enchantment.EnchantmentCost;
import io.fand.api.enchantment.EnchantmentDefinition;
import io.fand.api.enchantment.EnchantmentEffects;
import io.fand.api.enchantment.EnchantmentRegistry;
import io.fand.api.enchantment.EnchantmentRegistration;
import io.fand.api.enchantment.EnchantmentSlotGroup;
import io.fand.api.enchantment.EnchantmentView;
import io.fand.api.item.ItemStack;
import io.fand.api.registry.RegistryReference;
import io.fand.server.command.AdventureBridge;
import io.fand.server.item.FandItemStacks;
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
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;

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
                            enchantment.getMaxLevel(),
                            fromVanillaDefinition(enchantment.definition()),
                            encodeEffects(current, enchantment),
                            enchantment.exclusiveSet().unwrapKey()
                                    .map(tag -> List.of(RegistryReference.tag(apiKey(tag.location()))))
                                    .orElseGet(() -> enchantment.exclusiveSet().stream()
                                            .map(Holder::unwrapKey)
                                            .flatMap(Optional::stream)
                                            .map(ResourceKey::identifier)
                                            .map(FandEnchantmentRegistry::apiKey)
                                            .map(RegistryReference::key)
                                            .toList()));
                }));
    }

    @Override
    public boolean supports(Key enchantment, ItemStack item) {
        Objects.requireNonNull(enchantment, "enchantment");
        Objects.requireNonNull(item, "item");
        if (item.empty()) {
            return false;
        }
        var current = server.get();
        if (current == null) {
            return false;
        }
        return callOnServerThread(current, () -> current.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ResourceKey.create(Registries.ENCHANTMENT, identifier(enchantment)))
                .map(holder -> holder.value().isSupportedItem(FandItemStacks.toVanilla(item)))
                .orElse(false));
    }

    @Override
    public boolean compatible(Key first, Key second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first.equals(second)) {
            return true;
        }
        var current = server.get();
        if (current == null) {
            return false;
        }
        return callOnServerThread(current, () -> {
            var registry = current.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var firstHolder = registry.get(ResourceKey.create(Registries.ENCHANTMENT, identifier(first)));
            var secondHolder = registry.get(ResourceKey.create(Registries.ENCHANTMENT, identifier(second)));
            return firstHolder.isPresent() && secondHolder.isPresent()
                    && Enchantment.areCompatible(firstHolder.get(), secondHolder.get());
        });
    }

    @Override
    public EnchantmentRegistration register(CustomEnchantment enchantment) {
        Objects.requireNonNull(enchantment, "enchantment");
        var token = sequence.incrementAndGet();
        var view = new EnchantmentView(
                enchantment.key(),
                enchantment.description(),
                enchantment.maxLevel(),
                enchantment.definition(),
                enchantment.effects(),
                enchantment.exclusiveSet());
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
        return new Enchantment(
                AdventureBridge.toVanilla(custom.description(), server.registryAccess()),
                definition(server, custom.definition()),
                enchantmentSet(server, custom.exclusiveSet()),
                effects(server, custom.effects().toJson()));
    }

    private static Enchantment.EnchantmentDefinition definition(MinecraftServer server, EnchantmentDefinition definition) {
        var supportedItems = itemSet(server, definition.supportedItems());
        var primaryItems = definition.primaryItems();
        var slots = definition.slots().stream()
                .map(FandEnchantmentRegistry::slotGroup)
                .toArray(EquipmentSlotGroup[]::new);
        if (primaryItems != null) {
            return Enchantment.definition(
                    supportedItems,
                    itemSet(server, primaryItems),
                    definition.weight(),
                    definition.maxLevel(),
                    cost(definition.minCost()),
                    cost(definition.maxCost()),
                    definition.anvilCost(),
                    slots);
        }
        return Enchantment.definition(
                supportedItems,
                definition.weight(),
                definition.maxLevel(),
                cost(definition.minCost()),
                cost(definition.maxCost()),
                definition.anvilCost(),
                slots);
    }

    private static Enchantment.Cost cost(EnchantmentCost cost) {
        return Enchantment.dynamicCost(cost.base(), cost.perLevelAboveFirst());
    }

    private static HolderSet<Item> itemSet(MinecraftServer server, List<RegistryReference> references) {
        var items = server.registryAccess().lookupOrThrow(Registries.ITEM);
        var holders = new LinkedHashMap<Identifier, Holder<Item>>();
        for (var reference : references) {
            if (reference.tag() && reference.key().equals(net.kyori.adventure.key.Key.key("fand:all"))) {
                items.listElements()
                        .filter(holder -> holder.value() != Items.AIR)
                        .forEach(holder -> holders.put(holder.key().identifier(), (Holder<Item>) holder));
                continue;
            }
            if (reference.tag()) {
                var tag = TagKey.create(Registries.ITEM, identifier(reference.key()));
                var named = items.get(tag).orElseThrow(() -> new IllegalArgumentException("Unknown item tag: " + reference.asString()));
                named.stream().forEach(holder -> holders.put(holder.unwrapKey().orElseThrow().identifier(), holder));
            } else {
                var holder = items.get(ResourceKey.create(Registries.ITEM, identifier(reference.key())))
                        .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + reference.asString()));
                holders.put(holder.key().identifier(), holder);
            }
        }
        if (holders.isEmpty()) {
            throw new IllegalArgumentException("Enchantment item set resolved to empty: " + references);
        }
        return HolderSet.direct(holders.values().stream().toList());
    }

    private static HolderSet<Enchantment> enchantmentSet(MinecraftServer server, List<RegistryReference> references) {
        if (references.isEmpty()) {
            return HolderSet.empty();
        }
        var enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (references.size() == 1 && references.getFirst().tag()) {
            var tag = TagKey.create(Registries.ENCHANTMENT, identifier(references.getFirst().key()));
            return enchantments.get(tag).orElseThrow(() -> new IllegalArgumentException("Unknown enchantment tag: " + references.getFirst().asString()));
        }
        var holders = new LinkedHashMap<Identifier, Holder<Enchantment>>();
        for (var reference : references) {
            if (reference.tag()) {
                var tag = TagKey.create(Registries.ENCHANTMENT, identifier(reference.key()));
                var named = enchantments.get(tag).orElseThrow(() -> new IllegalArgumentException("Unknown enchantment tag: " + reference.asString()));
                named.stream().forEach(holder -> holders.put(holder.unwrapKey().orElseThrow().identifier(), holder));
            } else {
                var holder = enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, identifier(reference.key())))
                        .orElseThrow(() -> new IllegalArgumentException("Unknown enchantment: " + reference.asString()));
                holders.put(holder.key().identifier(), holder);
            }
        }
        return HolderSet.direct(holders.values().stream().toList());
    }

    private static net.minecraft.core.component.DataComponentMap effects(MinecraftServer server, JsonElement effects) {
        if (effects == null || effects.isJsonNull() || effects.getAsJsonObject().isEmpty()) {
            return net.minecraft.core.component.DataComponentMap.EMPTY;
        }
        return EnchantmentEffectComponents.CODEC.parse(ops(server), effects)
                .getOrThrow(error -> new IllegalArgumentException("Invalid enchantment effects: " + error));
    }

    private static EnchantmentEffects encodeEffects(MinecraftServer server, Enchantment enchantment) {
        var encoded = EnchantmentEffectComponents.CODEC.encodeStart(ops(server), enchantment.effects())
                .getOrThrow(error -> new IllegalArgumentException("Could not encode enchantment effects: " + error));
        return EnchantmentEffects.raw(encoded.isJsonObject() ? encoded.getAsJsonObject() : new com.google.gson.JsonObject());
    }

    private static RegistryOps<JsonElement> ops(MinecraftServer server) {
        return server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
    }

    private static EnchantmentDefinition fromVanillaDefinition(Enchantment.EnchantmentDefinition definition) {
        return new EnchantmentDefinition(
                itemReferences(definition.supportedItems()),
                definition.primaryItems()
                        .map(FandEnchantmentRegistry::itemReferences)
                        .orElse(null),
                definition.weight(),
                definition.maxLevel(),
                new EnchantmentCost(definition.minCost().base(), definition.minCost().perLevelAboveFirst()),
                new EnchantmentCost(definition.maxCost().base(), definition.maxCost().perLevelAboveFirst()),
                definition.anvilCost(),
                definition.slots().stream()
                        .map(FandEnchantmentRegistry::slotGroup)
                        .toList());
    }

    private static List<RegistryReference> itemReferences(HolderSet<Item> items) {
        return items.unwrapKey()
                .map(tag -> List.of(RegistryReference.tag(apiKey(tag.location()))))
                .orElseGet(() -> items.stream()
                        .map(Holder::unwrapKey)
                        .flatMap(Optional::stream)
                        .map(ResourceKey::identifier)
                        .map(FandEnchantmentRegistry::apiKey)
                        .map(RegistryReference::key)
                        .toList());
    }

    private static EquipmentSlotGroup slotGroup(EnchantmentSlotGroup slot) {
        return switch (slot) {
            case ANY -> EquipmentSlotGroup.ANY;
            case MAINHAND -> EquipmentSlotGroup.MAINHAND;
            case OFFHAND -> EquipmentSlotGroup.OFFHAND;
            case HAND -> EquipmentSlotGroup.HAND;
            case FEET -> EquipmentSlotGroup.FEET;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case HEAD -> EquipmentSlotGroup.HEAD;
            case ARMOR -> EquipmentSlotGroup.ARMOR;
            case BODY -> EquipmentSlotGroup.BODY;
            case SADDLE -> EquipmentSlotGroup.SADDLE;
        };
    }

    private static EnchantmentSlotGroup slotGroup(EquipmentSlotGroup slot) {
        return switch (slot) {
            case ANY -> EnchantmentSlotGroup.ANY;
            case MAINHAND -> EnchantmentSlotGroup.MAINHAND;
            case OFFHAND -> EnchantmentSlotGroup.OFFHAND;
            case HAND -> EnchantmentSlotGroup.HAND;
            case FEET -> EnchantmentSlotGroup.FEET;
            case LEGS -> EnchantmentSlotGroup.LEGS;
            case CHEST -> EnchantmentSlotGroup.CHEST;
            case HEAD -> EnchantmentSlotGroup.HEAD;
            case ARMOR -> EnchantmentSlotGroup.ARMOR;
            case BODY -> EnchantmentSlotGroup.BODY;
            case SADDLE -> EnchantmentSlotGroup.SADDLE;
        };
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static Key apiKey(Identifier id) {
        return Key.key(id.getNamespace(), id.getPath());
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
