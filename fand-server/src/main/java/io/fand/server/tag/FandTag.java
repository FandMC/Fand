package io.fand.server.tag;

import io.fand.api.tag.RegistryKind;
import io.fand.api.tag.Tag;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;

public final class FandTag<T, V> implements Tag<T> {

    private final HolderSet.Named<V> handle;
    private final RegistryKind registry;
    private final Function<V, T> wrapper;
    private final Function<T, Optional<Holder<V>>> holderResolver;
    private final Key key;

    public FandTag(
            HolderSet.Named<V> handle,
            RegistryKind registry,
            Function<V, T> wrapper,
            Function<T, Optional<Holder<V>>> holderResolver
    ) {
        this.handle = Objects.requireNonNull(handle, "handle");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.wrapper = Objects.requireNonNull(wrapper, "wrapper");
        this.holderResolver = Objects.requireNonNull(holderResolver, "holderResolver");
        var id = handle.key().location();
        this.key = Key.key(id.getNamespace(), id.getPath());
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public RegistryKind registry() {
        return registry;
    }

    @Override
    public Collection<? extends T> values() {
        return handle.stream()
                .map(Holder::value)
                .map(wrapper)
                .toList();
    }

    @Override
    public boolean contains(T value) {
        return holderResolver.apply(Objects.requireNonNull(value, "value"))
                .map(handle::contains)
                .orElse(false);
    }

    public boolean containsHolder(Holder<V> holder) {
        return handle.contains(holder);
    }

    @Override
    public int size() {
        return handle.size();
    }

    @Override
    public String toString() {
        return "FandTag(" + registry + " " + key.asString() + ")";
    }
}
