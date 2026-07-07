package io.fand.server.tag;

import io.fand.api.tag.TagRegistry;
import io.fand.api.tag.TagRegistryType;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

public final class FandTagRegistry implements TagRegistry {

    private final Supplier<@Nullable MinecraftServer> server;

    public FandTagRegistry(Supplier<@Nullable MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public Set<Key> tagsContaining(TagRegistryType registry, Key value) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(value, "value");
        return switch (registry) {
            case BLOCK -> tagsContaining(BuiltInRegistries.BLOCK, value);
            case ITEM -> tagsContaining(BuiltInRegistries.ITEM, value);
            case ENTITY_TYPE -> tagsContaining(BuiltInRegistries.ENTITY_TYPE, value);
            case FLUID -> tagsContaining(BuiltInRegistries.FLUID, value);
            case DAMAGE_TYPE -> dynamicDamageTypes()
                    .map(types -> tagsContaining(types, value))
                    .orElseGet(Set::of);
        };
    }

    @Override
    public Set<Key> values(TagRegistryType registry, Key tag) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(tag, "tag");
        return switch (registry) {
            case BLOCK -> values(BuiltInRegistries.BLOCK, FandTags.blockTagKey(tag));
            case ITEM -> values(BuiltInRegistries.ITEM, FandTags.itemTagKey(tag));
            case ENTITY_TYPE -> values(BuiltInRegistries.ENTITY_TYPE, FandTags.entityTypeTagKey(tag));
            case FLUID -> values(BuiltInRegistries.FLUID, FandTags.fluidTagKey(tag));
            case DAMAGE_TYPE -> dynamicDamageTypes()
                    .map(types -> values(types, FandTags.damageTypeTagKey(tag)))
                    .orElseGet(Set::of);
        };
    }

    @Override
    public Collection<Key> tags(TagRegistryType registry) {
        Objects.requireNonNull(registry, "registry");
        return switch (registry) {
            case BLOCK -> tags(BuiltInRegistries.BLOCK);
            case ITEM -> tags(BuiltInRegistries.ITEM);
            case ENTITY_TYPE -> tags(BuiltInRegistries.ENTITY_TYPE);
            case FLUID -> tags(BuiltInRegistries.FLUID);
            case DAMAGE_TYPE -> dynamicDamageTypes()
                    .map(FandTagRegistry::tags)
                    .orElseGet(Set::of);
        };
    }

    private java.util.Optional<Registry<net.minecraft.world.damagesource.DamageType>> dynamicDamageTypes() {
        var current = server.get();
        return current == null
                ? java.util.Optional.empty()
                : current.registryAccess().lookup(Registries.DAMAGE_TYPE);
    }

    private static <T> Set<Key> tagsContaining(Registry<T> registry, Key value) {
        return registry.get(identifier(value))
                .map(holder -> holder.tags()
                        .map(TagKey::location)
                        .map(FandTagRegistry::key)
                        .collect(Collectors.toUnmodifiableSet()))
                .orElseGet(Set::of);
    }

    private static <T> Set<Key> values(Registry<T> registry, TagKey<T> tag) {
        return registry.get(tag)
                .map(HolderSet::stream)
                .orElseGet(java.util.stream.Stream::of)
                .map(Holder::unwrapKey)
                .flatMap(java.util.Optional::stream)
                .map(resourceKey -> resourceKey.identifier())
                .map(FandTagRegistry::key)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static <T> Set<Key> tags(Registry<T> registry) {
        return registry.getTags()
                .map(HolderSet.Named::key)
                .map(TagKey::location)
                .map(FandTagRegistry::key)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static Key key(Identifier id) {
        return Key.key(id.getNamespace(), id.getPath());
    }
}
