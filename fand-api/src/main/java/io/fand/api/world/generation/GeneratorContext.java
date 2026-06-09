package io.fand.api.world.generation;

import net.kyori.adventure.key.Key;

/**
 * Immutable metadata available to world generation callbacks.
 */
public final class GeneratorContext {

    private final long seed;
    private final Key world;
    private final ChunkGenerationStage stage;

    public GeneratorContext(long seed, Key world, ChunkGenerationStage stage) {
        this.seed = seed;
        this.world = java.util.Objects.requireNonNull(world, "world");
        this.stage = java.util.Objects.requireNonNull(stage, "stage");
    }

    public long seed() {
        return seed;
    }

    public Key world() {
        return world;
    }

    public ChunkGenerationStage stage() {
        return stage;
    }

    public long chunkSeed(int chunkX, int chunkZ) {
        long x = chunkX * 341873128712L;
        long z = chunkZ * 132897987541L;
        return seed ^ x ^ z;
    }

    public long columnSeed(int x, int z) {
        long a = x * 3129871L;
        long b = z * 116129781L;
        long mixed = a ^ b ^ seed;
        return mixed * mixed * 42317861L + mixed * 11L;
    }

    public static GeneratorContext unknown() {
        return new GeneratorContext(0L, Key.key("minecraft:overworld"), ChunkGenerationStage.NOISE);
    }
}
