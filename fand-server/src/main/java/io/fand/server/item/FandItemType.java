package io.fand.server.item;

import io.fand.api.item.ItemType;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.minecraft.world.item.Item;

public final class FandItemType implements ItemType {

    private static final ConcurrentHashMap<Item, FandItemType> CACHE = new ConcurrentHashMap<>();

    private final Item handle;
    private final Key key;

    private FandItemType(Item handle) {
        this.handle = handle;
        var id = handle.builtInRegistryHolder().key().identifier();
        this.key = Key.key(id.getNamespace(), id.getPath());
    }

    public static FandItemType of(Item handle) {
        var existing = CACHE.get(handle);
        return existing != null ? existing : CACHE.computeIfAbsent(handle, FandItemType::new);
    }

    public Item handle() {
        return handle;
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public int maxStackSize() {
        return handle.getDefaultMaxStackSize();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandItemType that && this.handle == that.handle;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "FandItemType(" + key.asString() + ")";
    }
}
