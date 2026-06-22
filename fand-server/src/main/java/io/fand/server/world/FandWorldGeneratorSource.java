package io.fand.server.world;

import com.mojang.serialization.MapCodec;
import io.fand.api.world.HeightmapType;
import io.fand.api.world.WorldGenerator;
import io.fand.api.world.generation.ChunkGenerationStage;
import io.fand.api.world.generation.DecorationStep;
import io.fand.api.world.generation.GenerationMode;
import io.fand.api.world.generation.GeneratorContext;
import io.fand.api.world.generation.WorldGeneratorSettings;
import io.fand.server.structure.FandStructureService;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public final class FandWorldGeneratorSource extends ChunkGenerator {

    private final Key world;
    private final long seed;
    private final HolderLookup.RegistryLookup<Biome> biomes;
    private final WorldGenerator generator;
    private final WorldGeneratorSettings settings;
    private final FandStructureService structures;

    public FandWorldGeneratorSource(
            Key world,
            long seed,
            HolderLookup.RegistryLookup<Biome> biomes,
            WorldGenerator generator,
            WorldGeneratorSettings settings,
            FandStructureService structures
    ) {
        super(
                new FandBiomeSource(biomes, settings.biomeProvider(), biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS)),
                generationSettingsGetter(settings));
        this.world = Objects.requireNonNull(world, "world");
        this.seed = seed;
        this.biomes = Objects.requireNonNull(biomes, "biomes");
        this.generator = Objects.requireNonNull(generator, "generator");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.structures = Objects.requireNonNull(structures, "structures");
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
                new FilteredStructureSetLookup(structureSets, settings, structures));
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess centerChunk
    ) {
        if (settings.mode() != GenerationMode.EMPTY) {
            var context = context(ChunkGenerationStage.NOISE);
            generator.generate(new FandGeneratedChunk(centerChunk, context, biomes), context);
        }
        return CompletableFuture.completedFuture(centerChunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        int fallback = settings.mode() == GenerationMode.EMPTY ? heightAccessor.getMinY() : scanBaseHeight(x, z, type, heightAccessor);
        return generator.baseHeight(x, z, heightmap(type), context(ChunkGenerationStage.NOISE), fallback);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        return emptyColumn(heightAccessor);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess protoChunk) {
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
            ChunkPos center = worldGenRegion.getCenter();
            Holder<Biome> biome = worldGenRegion.getBiome(center.getWorldPosition().atY(worldGenRegion.getMaxY()));
            WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
            random.setDecorationSeed(worldGenRegion.getSeed(), center.getMinBlockX(), center.getMinBlockZ());
            NaturalSpawner.spawnMobsForChunkGeneration(worldGenRegion, biome, center, random);
        }
        generator.spawnOriginalMobs(context(ChunkGenerationStage.SPAWN));
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos) {
        generator.addDebugInfo(
                result,
                context(ChunkGenerationStage.FULL),
                feetPos.getX(),
                feetPos.getY(),
                feetPos.getZ());
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor heightAccessor) {
        return settings.spawnHeight().orElseGet(() -> Math.min(heightAccessor.getMaxY(), Math.max(heightAccessor.getMinY(), settings.seaLevel() + 1)));
    }

    @Override
    public int getMinY() {
        return settings.minY();
    }

    @Override
    public int getGenDepth() {
        return settings.height();
    }

    @Override
    public int getSeaLevel() {
        return settings.seaLevel();
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

    private int scanBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor) {
        return heightAccessor.getMinY();
    }

    static NoiseColumn emptyColumn(LevelHeightAccessor heightAccessor) {
        var states = new BlockState[heightAccessor.getHeight()];
        java.util.Arrays.fill(states, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        return new NoiseColumn(heightAccessor.getMinY(), states);
    }

    static Function<Holder<Biome>, BiomeGenerationSettings> generationSettingsGetter(WorldGeneratorSettings settings) {
        return biome -> generationSettings(settings, biome);
    }

    private static BiomeGenerationSettings generationSettings(WorldGeneratorSettings settings, Holder<Biome> biome) {
        if (!settings.inheritVanillaBiomeFeatures() && !settings.inheritVanillaBiomeCarvers()) {
            return BiomeGenerationSettings.EMPTY;
        }
        var builder = new BiomeGenerationSettings.PlainBuilder();
        var generation = biome.value().getGenerationSettings();
        if (settings.inheritVanillaBiomeCarvers()) {
            for (var carver : generation.getCarvers()) {
                builder.addCarver(carver);
            }
        }
        if (settings.inheritVanillaBiomeFeatures()) {
            var source = generation.features();
            for (var step : settings.decorationSteps()) {
                int index = step.ordinal();
                if (index >= source.size()) {
                    continue;
                }
                for (var feature : source.get(index)) {
                    builder.addFeature(index, feature);
                }
            }
        }
        return builder.build();
    }

    static final class FilteredStructureSetLookup implements HolderLookup.RegistryLookup<StructureSet> {

        private final HolderLookup<StructureSet> parent;
        private final WorldGeneratorSettings settings;
        private final FandStructureService structures;

        FilteredStructureSetLookup(HolderLookup<StructureSet> parent, WorldGeneratorSettings settings, FandStructureService structures) {
            this.parent = Objects.requireNonNull(parent, "parent");
            this.settings = Objects.requireNonNull(settings, "settings");
            this.structures = Objects.requireNonNull(structures, "structures");
        }

        @Override
        public ResourceKey<? extends net.minecraft.core.Registry<? extends StructureSet>> key() {
            return Registries.STRUCTURE_SET;
        }

        @Override
        public com.mojang.serialization.Lifecycle registryLifecycle() {
            return com.mojang.serialization.Lifecycle.stable();
        }

        @Override
        public java.util.Optional<Holder.Reference<StructureSet>> get(ResourceKey<StructureSet> id) {
            return parent.get(id)
                    .or(() -> structures.structureSetHolders().filter(holder -> holder.key().equals(id)).findFirst())
                    .filter(this::enabled);
        }

        @Override
        public java.util.stream.Stream<Holder.Reference<StructureSet>> listElements() {
            return java.util.stream.Stream.concat(
                    parent.listElements()
                            .filter(holder -> !structures.runtimeStructureSetOwned(apiKey(holder.key())))
                            .filter(this::enabled),
                    structures.structureSetHolders().filter(this::enabled));
        }

        @Override
        public java.util.Optional<HolderSet.Named<StructureSet>> get(TagKey<StructureSet> id) {
            return parent.get(id);
        }

        @Override
        public java.util.stream.Stream<HolderSet.Named<StructureSet>> listTags() {
            return parent.listTags();
        }

        private boolean enabled(Holder.Reference<StructureSet> holder) {
            var apiKey = apiKey(holder.key());
            return settings.structureSetEnabled(apiKey)
                    && structures.runtimeStructureSetActive(apiKey)
                    && structureSetReferencesActive(holder);
        }

        private boolean structureSetReferencesActive(Holder.Reference<StructureSet> holder) {
            if (!structures.runtimeStructureSetOwned(apiKey(holder.key()))) {
                return true;
            }
            return holder.value().structures().stream()
                    .allMatch(entry -> entry.structure().unwrapKey()
                            .map(FandWorldGeneratorSource.FilteredStructureSetLookup::apiKey)
                            .map(structures::runtimeStructureActive)
                            .orElse(true));
        }

        private static Key apiKey(ResourceKey<?> key) {
            var identifier = key.identifier();
            return Key.key(identifier.getNamespace(), identifier.getPath());
        }
    }
}
