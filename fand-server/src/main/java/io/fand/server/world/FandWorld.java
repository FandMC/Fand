package io.fand.server.world;

import io.fand.api.block.Block;
import io.fand.api.world.World;
import io.fand.server.block.FandBlock;
import net.kyori.adventure.key.Key;
import net.minecraft.server.level.ServerLevel;

public final class FandWorld implements World {

    private final ServerLevel handle;
    private final Key key;

    public FandWorld(ServerLevel handle) {
        this.handle = handle;
        var identifier = handle.dimension().identifier();
        this.key = Key.key(identifier.getNamespace(), identifier.getPath());
    }

    public ServerLevel handle() {
        return handle;
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public long seed() {
        return handle.getSeed();
    }

    @Override
    public Block blockAt(int x, int y, int z) {
        return new FandBlock(this, x, y, z);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandWorld that && this.key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "FandWorld(" + key.asString() + ")";
    }
}
