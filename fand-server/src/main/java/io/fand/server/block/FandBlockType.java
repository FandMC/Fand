package io.fand.server.block;

import io.fand.api.block.BlockType;
import net.kyori.adventure.key.Key;
import net.minecraft.world.level.block.Block;

public final class FandBlockType implements BlockType {

    private final Block handle;
    private final Key key;

    public FandBlockType(Block handle) {
        this.handle = handle;
        var id = handle.builtInRegistryHolder().key().identifier();
        this.key = Key.key(id.getNamespace(), id.getPath());
    }

    public Block handle() {
        return handle;
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandBlockType that && this.handle == that.handle;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "FandBlockType(" + key.asString() + ")";
    }
}
