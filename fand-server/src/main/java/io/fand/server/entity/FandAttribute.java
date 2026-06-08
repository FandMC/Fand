package io.fand.server.entity;

import java.util.Objects;
import net.kyori.adventure.key.Key;
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
}
