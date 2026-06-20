package io.fand.server.entity;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public final class FandAttribute implements io.fand.api.entity.Attribute {

    private final AttributeInstance handle;
    private final java.util.function.Consumer<Runnable> scheduler;

    public FandAttribute(AttributeInstance handle, java.util.function.Consumer<Runnable> scheduler) {
        this.handle = Objects.requireNonNull(handle, "handle");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public Key key() {
        var id = BuiltInRegistries.ATTRIBUTE.getKey(handle.getAttribute().value());
        if (id == null) {
            return Key.key("minecraft:unknown");
        }
        return Key.key(id.getNamespace(), id.getPath());
    }

    @Override
    public double value() {
        return handle.getValue();
    }

    @Override
    public double baseValue() {
        return handle.getBaseValue();
    }

    @Override
    public void setBaseValue(double value) {
        scheduler.accept(() -> handle.setBaseValue(value));
    }

    @Override
    public double defaultValue() {
        return handle.getAttribute().value().getDefaultValue();
    }

    @Override
    public Set<io.fand.api.entity.AttributeModifier> modifiers() {
        return handle.getModifiers().stream()
                .map(modifier -> toApi(modifier, handle.getPermanentModifiers().contains(modifier)))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<io.fand.api.entity.AttributeModifier> modifier(Key key) {
        Objects.requireNonNull(key, "key");
        var vanilla = handle.getModifier(identifier(key));
        return vanilla == null
                ? Optional.empty()
                : Optional.of(toApi(vanilla, handle.getPermanentModifiers().contains(vanilla)));
    }

    @Override
    public boolean hasModifier(Key key) {
        Objects.requireNonNull(key, "key");
        return handle.hasModifier(identifier(key));
    }

    @Override
    public void addModifier(io.fand.api.entity.AttributeModifier modifier) {
        Objects.requireNonNull(modifier, "modifier");
        var vanilla = toVanilla(modifier);
        scheduler.accept(() -> {
            if (modifier.persistent()) {
                handle.addOrReplacePermanentModifier(vanilla);
            } else {
                handle.removeModifier(vanilla.id());
                handle.addOrUpdateTransientModifier(vanilla);
            }
        });
    }

    @Override
    public boolean removeModifier(Key key) {
        Objects.requireNonNull(key, "key");
        var id = identifier(key);
        var present = handle.hasModifier(id);
        if (present) {
            scheduler.accept(() -> handle.removeModifier(id));
        }
        return present;
    }

    private static io.fand.api.entity.AttributeModifier toApi(
            net.minecraft.world.entity.ai.attributes.AttributeModifier modifier,
            boolean persistent
    ) {
        return new io.fand.api.entity.AttributeModifier(
                key(modifier.id()),
                modifier.amount(),
                switch (modifier.operation()) {
                    case ADD_VALUE -> io.fand.api.entity.AttributeModifierOperation.ADD_VALUE;
                    case ADD_MULTIPLIED_BASE -> io.fand.api.entity.AttributeModifierOperation.ADD_MULTIPLIED_BASE;
                    case ADD_MULTIPLIED_TOTAL -> io.fand.api.entity.AttributeModifierOperation.ADD_MULTIPLIED_TOTAL;
                },
                persistent);
    }

    private static net.minecraft.world.entity.ai.attributes.AttributeModifier toVanilla(
            io.fand.api.entity.AttributeModifier modifier
    ) {
        return new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                identifier(modifier.key()),
                modifier.amount(),
                switch (modifier.operation()) {
                    case ADD_VALUE -> net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
                    case ADD_MULTIPLIED_BASE -> net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    case ADD_MULTIPLIED_TOTAL -> net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                });
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static Key key(Identifier id) {
        return Key.key(id.getNamespace(), id.getPath());
    }
}
