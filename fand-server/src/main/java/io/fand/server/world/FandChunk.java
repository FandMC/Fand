package io.fand.server.world;

import io.fand.api.world.Chunk;
import io.fand.api.world.World;

public final class FandChunk implements Chunk {

    private final FandWorld world;
    private final int x;
    private final int z;

    public FandChunk(FandWorld world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    @Override
    public World world() {
        return world;
    }

    @Override
    public int x() {
        return x;
    }

    @Override
    public int z() {
        return z;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandChunk that
                && this.x == that.x
                && this.z == that.z
                && this.world.key().equals(that.world.key());
    }

    @Override
    public int hashCode() {
        int result = world.key().hashCode();
        result = 31 * result + x;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString() {
        return "FandChunk(" + world.key().asString() + " " + x + "," + z + ")";
    }
}
