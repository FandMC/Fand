package io.fand.api.world;

import java.util.Objects;

record WorldChunk(World world, int x, int z) implements Chunk {

    WorldChunk {
        Objects.requireNonNull(world, "world");
    }
}
