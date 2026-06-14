package io.fand.server.entity;

import io.fand.api.entity.EntityType;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityTypes;

public final class FandEntityType implements EntityType {

    private static final ConcurrentHashMap<net.minecraft.world.entity.EntityType<?>, FandEntityType> CACHE =
            new ConcurrentHashMap<>();

    private final net.minecraft.world.entity.EntityType<?> handle;
    private final Key key;

    private FandEntityType(net.minecraft.world.entity.EntityType<?> handle) {
        this.handle = handle;
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(handle);
        this.key = Key.key(id.getNamespace(), id.getPath());
    }

    public static FandEntityType of(net.minecraft.world.entity.EntityType<?> handle) {
        var existing = CACHE.get(handle);
        return existing != null ? existing : CACHE.computeIfAbsent(handle, FandEntityType::new);
    }

    public net.minecraft.world.entity.EntityType<?> handle() {
        return handle;
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public boolean spawnable() {
        return handle != EntityTypes.PLAYER;
    }

    @Override
    public boolean player() {
        return handle == EntityTypes.PLAYER;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandEntityType that && this.handle == that.handle;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "FandEntityType(" + key.asString() + ")";
    }
}
