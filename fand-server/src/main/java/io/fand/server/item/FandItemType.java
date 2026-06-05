package io.fand.server.item;

import io.fand.api.item.ItemType;
import net.kyori.adventure.key.Key;
import net.minecraft.world.item.Item;

public final class FandItemType implements ItemType {

    private final Item handle;
    private final Key key;

    public FandItemType(Item handle) {
        this.handle = handle;
        var id = handle.builtInRegistryHolder().key().identifier();
        this.key = Key.key(id.getNamespace(), id.getPath());
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
