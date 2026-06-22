package io.fand.server.world;

import com.mojang.serialization.MapCodec;
import com.mojang.datafixers.util.Pair;
import io.fand.api.world.HeightmapType;
import io.fand.api.world.WorldGenerator;
import io.fand.api.world.generation.ChunkGenerationStage;
import io.fand.api.world.generation.GeneratorContext;
import io.fand.api.world.generation.WorldGeneratorSettings;
import io.fand.server.structure.FandStructureService;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.util.random.WeightedList;
import org.jspecify.annotations.Nullable;

public final class FandVanillaWorldGeneratorSource extends ChunkGenerator implements FandNoiseSettingsSource {

    private final Key world;
    private final long seed;
    private final HolderLookup.RegistryLookup<Biome> biomes;
    private final NoiseBasedChunkGenerator delegate;
    private final WorldGenerator generator;
    private final WorldGeneratorSettings settings;
    private final FandStructureService structures;

    public FandVanillaWorldGeneratorSource(
            Key world,
            long seed,
            HolderLookup.RegistryLookup<Biome> biomes,
            BiomeSource biomeSource,
            Holder<NoiseGeneratorSettings> noiseSettings,
            WorldGenerator generator,
            WorldGeneratorSettings settings,
            FandStructureService structures
    ) {
        super(biomeSource, FandWorldGeneratorSource.generationSettingsGetter(settings));
        this.world = Objects.requireNonNull(world, "world");
        this.seed = seed;
        this.biomes = Objects.requireNonNull(biomes, "biomes");
        this.delegate = new NoiseBasedChunkGenerator(biomeSource, Objects.requireNonNull(noiseSettings, "noiseSettings"));
        this.generator = Objects.requireNonNull(generator, "generator");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.structures = Objects.requireNonNull(structures, "structures");
    }

    @Override
    public Holder<NoiseGeneratorSettings> fand$noiseGeneratorSettings() {
        return delegate.generatorSettings();
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return MapCodec.unit(this);
    }

    @Override
    public ChunkGeneratorStructureState createState(
            HolderLookup<StructureSet> structureSets,
            RandomState randomState,
            long legacyLevelSeed
    ) {
        if (!settings.generateStructures()) {
            return ChunkGeneratorStructureState.createForFlat(randomState, legacyLevelSeed, biomeSource, java.util.stream.Stream.empty());
        }
        return ChunkGeneratorStructureState.createForNormal(
                randomState,
                legacyLevelSeed,
                biomeSource,
                new FandWorldGeneratorSource.FilteredStructureSetLookup(structureSets, settings, structures));
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(
            RandomState randomState,
            Blender blender,
            StructureManager structureManager,
            ChunkAccess protoChunk
    ) {
        return delegate.createBiomes(randomState, blender, structureManager, protoChunk);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess centerChunk
    ) {
        CompletableFuture<ChunkAccess> base = settings.generateNoise()
                ? delegate.fillFromNoise(blender, randomState, structureManager, centerChunk)
                : CompletableFuture.completedFuture(centerChunk);
        return base.thenApply(chunk -> {
            var context = context(ChunkGenerationStage.NOISE);
            generator.generate(new FandGeneratedChunk(chunk, context, biomes), context);
            return chunk;
        });
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        int fallback = settings.generateNoise()
                ? delegate.getBaseHeight(x, z, type, heightAccessor, randomState)
                : heightAccessor.getMinY();
        return generator.baseHeight(x, z, heightmap(type), context(ChunkGenerationStage.NOISE), fallback);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        return settings.generateNoise()
                ? delegate.getBaseColumn(x, z, heightAccessor, randomState)
                : FandWorldGeneratorSource.emptyColumn(heightAccessor);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess protoChunk) {
        if (settings.generateSurface()) {
            delegate.buildSurface(level, structureManager, randomState, protoChunk);
        }
        var context = context(ChunkGenerationStage.SURFACE);
        generator.buildSurface(new FandGeneratedChunk(protoChunk, context, biomes), context);
    }

    @Override
    public void applyCarvers(
            WorldGenRegion region,
            long seed,
            RandomState randomState,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk
    ) {
        if (settings.inheritVanillaBiomeCarvers()) {
            delegate.applyCarvers(region, seed, randomState, biomeManager, structureManager, chunk);
        }
        if (settings.generateCarvers()) {
            var context = context(ChunkGenerationStage.CARVERS);
            generator.carve(new FandGeneratedChunk(chunk, context, biomes), context);
        }
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        if (settings.generateStructures() || settings.inheritVanillaBiomeFeatures()) {
            super.applyBiomeDecoration(level, chunk, structureManager);
        }
        if (settings.generateDecorations()) {
            var context = context(ChunkGenerationStage.FEATURES);
            var generated = new FandGeneratedChunk(chunk, context, biomes);
            for (var step : settings.decorationSteps()) {
                generator.decorate(generated, step, context);
            }
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
        if (settings.spawnOriginalMobs()) {
            delegate.spawnOriginalMobs(worldGenRegion);
        }
        generator.spawnOriginalMobs(context(ChunkGenerationStage.SPAWN));
    }

    @Override
    public @Nullable Pair<BlockPos, Holder<Structure>> findNearestMapStructure(
            ServerLevel level,
            HolderSet<Structure> wantedStructures,
            BlockPos pos,
            int maxSearchRadius,
            boolean createReference
    ) {
        if (!settings.generateStructures()) {
            return null;
        }
        return super.findNearestMapStructure(level, wantedStructures, pos, maxSearchRadius, createReference);
    }

    @Override
    public WeightedList<MobSpawnSettings.SpawnerData> getMobsAt(
            Holder<Biome> biome,
            StructureManager structureManager,
            MobCategory mobCategory,
            BlockPos pos
    ) {
        return delegate.getMobsAt(biome, structureManager, mobCategory, pos);
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos) {
        delegate.addDebugScreenInfo(result, randomState, feetPos);
        generator.addDebugInfo(
                result,
                context(ChunkGenerationStage.FULL),
                feetPos.getX(),
                feetPos.getY(),
                feetPos.getZ());
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor heightAccessor) {
        return settings.spawnHeight().orElseGet(() -> delegate.getSpawnHeight(heightAccessor));
    }

    @Override
    public int getGenDepth() {
        return delegate.getGenDepth();
    }

    @Override
    public int getSeaLevel() {
        return delegate.getSeaLevel();
    }

    @Override
    public int getMinY() {
        return delegate.getMinY();
    }

    private GeneratorContext context(ChunkGenerationStage stage) {
        return new GeneratorContext(seed, world, stage);
    }

    private static HeightmapType heightmap(Heightmap.Types type) {
        return switch (type) {
            case WORLD_SURFACE_WG -> HeightmapType.WORLD_SURFACE_WG;
            case WORLD_SURFACE -> HeightmapType.WORLD_SURFACE;
            case OCEAN_FLOOR_WG -> HeightmapType.OCEAN_FLOOR_WG;
            case OCEAN_FLOOR -> HeightmapType.OCEAN_FLOOR;
            case MOTION_BLOCKING -> HeightmapType.MOTION_BLOCKING;
            case MOTION_BLOCKING_NO_LEAVES -> HeightmapType.MOTION_BLOCKING_NO_LEAVES;
        };
    }
}
