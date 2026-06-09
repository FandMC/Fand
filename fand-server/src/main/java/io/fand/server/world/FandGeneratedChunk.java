package io.fand.server.world;

import io.fand.api.block.BlockType;
import io.fand.api.world.BlockUpdateMode;
import io.fand.api.world.GeneratedChunk;
import io.fand.api.world.generation.GeneratorContext;
import io.fand.server.block.FandBlockType;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;

final class FandGeneratedChunk implements GeneratedChunk {

    private final ChunkAccess handle;
    private final GeneratorContext context;
    private final HolderLookup.RegistryLookup<Biome> biomes;
    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    FandGeneratedChunk(ChunkAccess handle, GeneratorContext context) {
        this(handle, context, null);
    }

    FandGeneratedChunk(
            ChunkAccess handle,
            GeneratorContext context,
            HolderLookup.RegistryLookup<Biome> biomes
    ) {
        this.handle = Objects.requireNonNull(handle, "handle");
        this.context = Objects.requireNonNull(context, "context");
        this.biomes = biomes;
    }

    @Override
    public int chunkX() {
        return handle.getPos().x();
    }

    @Override
    public int chunkZ() {
        return handle.getPos().z();
    }

    @Override
    public int minY() {
        return handle.getMinY();
    }

    @Override
    public int maxY() {
        return handle.getMaxY();
    }

    @Override
    public GeneratorContext context() {
        return context;
    }

    @Override
    public BlockType blockType(int x, int y, int z) {
        if (!contains(x, y, z)) {
            throw new IllegalArgumentException("Generated block is outside chunk " + chunkX() + "," + chunkZ());
        }
        return FandBlockType.of(handle.getBlockState(mutablePos.set(x, y, z)).getBlock());
    }

    @Override
    public Key biomeAt(int x, int y, int z) {
        if (!contains(x, y, z)) {
            throw new IllegalArgumentException("Generated biome is outside chunk " + chunkX() + "," + chunkZ());
        }
        var key = handle.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z))
                .unwrapKey()
                .map(ResourceKey::identifier)
                .orElse(Identifier.withDefaultNamespace("plains"));
        return Key.key(key.getNamespace(), key.getPath());
    }

    @Override
    public void setBiome(int x, int y, int z, Key biome) {
        Objects.requireNonNull(biome, "biome");
        if (!contains(x, y, z)) {
            throw new IllegalArgumentException("Generated biome is outside chunk " + chunkX() + "," + chunkZ());
        }
        if (biomes == null) {
            throw new UnsupportedOperationException("Biome writes are only available during biome-aware generation stages");
        }
        int targetQuartX = QuartPos.fromBlock(x);
        int targetQuartY = QuartPos.fromBlock(y);
        int targetQuartZ = QuartPos.fromBlock(z);
        var target = biomes.get(ResourceKey.create(
                        Registries.BIOME,
                        Identifier.fromNamespaceAndPath(biome.namespace(), biome.value())))
                .orElseThrow(() -> new IllegalArgumentException("Biome is not available: " + biome.asString()));
        handle.fillBiomesFromNoise(
                (quartX, quartY, quartZ, sampler) -> quartX == targetQuartX && quartY == targetQuartY && quartZ == targetQuartZ
                        ? target
                        : handle.getNoiseBiome(quartX, quartY, quartZ),
                Climate.empty());
    }

    @Override
    public void setBlock(int x, int y, int z, BlockType type, BlockUpdateMode updateMode) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(updateMode, "updateMode");
        if (!contains(x, y, z)) {
            throw new IllegalArgumentException("Generated block is outside chunk " + chunkX() + "," + chunkZ());
        }
        handle.setBlockState(mutablePos.set(x, y, z), FandBlockType.unwrap(type).defaultBlockState(), updateFlags(updateMode));
    }

    private static int updateFlags(BlockUpdateMode mode) {
        return switch (mode) {
            case NORMAL -> Block.UPDATE_ALL;
            case CLIENTS_ONLY -> Block.UPDATE_CLIENTS;
            case SILENT -> Block.UPDATE_NONE;
        };
    }
}
