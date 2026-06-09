package io.fand.server.block;

import io.fand.api.block.BlockType;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.minecraft.world.level.block.Block;

public final class FandBlockType implements BlockType {

    private static final ConcurrentHashMap<Block, FandBlockType> CACHE = new ConcurrentHashMap<>();

    private final Block handle;
    private final Key key;

    private FandBlockType(Block handle) {
        this.handle = handle;
        var id = handle.builtInRegistryHolder().key().identifier();
        this.key = Key.key(id.getNamespace(), id.getPath());
    }

    public static FandBlockType of(Block handle) {
        var existing = CACHE.get(handle);
        return existing != null ? existing : CACHE.computeIfAbsent(handle, FandBlockType::new);
    }

    public Block handle() {
        return handle;
    }

    public static Block unwrap(BlockType type) {
        if (type instanceof FandBlockType fand) {
            return fand.handle;
        }
        throw new IllegalArgumentException("Block type is not owned by this server: " + type.key().asString());
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
