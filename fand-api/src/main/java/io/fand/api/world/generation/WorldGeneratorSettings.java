package io.fand.api.world.generation;

import io.fand.api.VanillaKey;
import io.fand.api.world.BiomeKey;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Runtime settings for a custom dynamic world chunk generator.
 */
public final class WorldGeneratorSettings {

    private final GenerationMode mode;
    private final BiomeProvider biomeProvider;
    private final VanillaBiomeSource biomeSource;
    private final VanillaBiomeGeneration vanillaBiomeGeneration;
    private final @Nullable Key noiseSettings;
    private final boolean generateNoise;
    private final boolean generateSurface;
    private final boolean generateStructures;
    private final boolean generateDecorations;
    private final boolean generateCarvers;
    private final boolean spawnOriginalMobs;
    private final EnumSet<DecorationStep> decorationSteps;
    private final Set<Key> includedStructureSets;
    private final Set<Key> excludedStructureSets;
    private final @Nullable Key dimensionType;
    private final int seaLevel;
    private final int minY;
    private final int height;
    private final @Nullable Integer spawnHeight;

    private WorldGeneratorSettings(
            GenerationMode mode,
            BiomeProvider biomeProvider,
            VanillaBiomeSource biomeSource,
            VanillaBiomeGeneration vanillaBiomeGeneration,
            @Nullable Key noiseSettings,
            boolean generateNoise,
            boolean generateSurface,
            boolean generateStructures,
            boolean generateDecorations,
            boolean generateCarvers,
            boolean spawnOriginalMobs,
            EnumSet<DecorationStep> decorationSteps,
            Set<Key> includedStructureSets,
            Set<Key> excludedStructureSets,
            @Nullable Key dimensionType,
            int seaLevel,
            int minY,
            int height,
            @Nullable Integer spawnHeight
    ) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.biomeProvider = Objects.requireNonNull(biomeProvider, "biomeProvider");
        this.biomeSource = Objects.requireNonNull(biomeSource, "biomeSource");
        this.vanillaBiomeGeneration = Objects.requireNonNull(vanillaBiomeGeneration, "vanillaBiomeGeneration");
        this.noiseSettings = noiseSettings;
        this.generateNoise = generateNoise;
        this.generateSurface = generateSurface;
        this.generateStructures = generateStructures;
        this.generateDecorations = generateDecorations;
        this.generateCarvers = generateCarvers;
        this.spawnOriginalMobs = spawnOriginalMobs;
        this.decorationSteps = decorationSteps.clone();
        this.includedStructureSets = Set.copyOf(includedStructureSets);
        this.excludedStructureSets = Set.copyOf(excludedStructureSets);
        this.dimensionType = dimensionType;
        this.seaLevel = seaLevel;
        this.minY = minY;
        this.height = height;
        this.spawnHeight = spawnHeight;
        if (height <= 0 || (height & 15) != 0) {
            throw new IllegalArgumentException("height must be positive and divisible by 16");
        }
        if (spawnHeight != null && (spawnHeight < minY || spawnHeight > maxY())) {
            throw new IllegalArgumentException("spawnHeight must be inside the world height");
        }
    }

    public static WorldGeneratorSettings empty() {
        return builder().mode(GenerationMode.EMPTY).build();
    }

    public static WorldGeneratorSettings custom() {
        return builder().mode(GenerationMode.CUSTOM).build();
    }

    public static WorldGeneratorSettings vanilla() {
        return builder().mode(GenerationMode.VANILLA)
                .biomeSource(VanillaBiomeSource.TEMPLATE)
                .vanillaBiomeGeneration(VanillaBiomeGeneration.FEATURES_AND_CARVERS)
                .generateNoise(true)
                .generateSurface(true)
                .generateStructures(true)
                .generateDecorations(false)
                .generateCarvers(false)
                .spawnOriginalMobs(true)
                .build();
    }

    public static WorldGeneratorSettings template() {
        return builder().mode(GenerationMode.TEMPLATE).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public GenerationMode mode() {
        return mode;
    }

    public BiomeProvider biomeProvider() {
        return biomeProvider;
    }

    public VanillaBiomeSource biomeSource() {
        return biomeSource;
    }

    public VanillaBiomeGeneration vanillaBiomeGeneration() {
        return vanillaBiomeGeneration;
    }

    public boolean inheritVanillaBiomeFeatures() {
        return vanillaBiomeGeneration == VanillaBiomeGeneration.FEATURES
                || vanillaBiomeGeneration == VanillaBiomeGeneration.FEATURES_AND_CARVERS;
    }

    public boolean inheritVanillaBiomeCarvers() {
        return vanillaBiomeGeneration == VanillaBiomeGeneration.CARVERS
                || vanillaBiomeGeneration == VanillaBiomeGeneration.FEATURES_AND_CARVERS;
    }

    public Optional<Key> noiseSettings() {
        return Optional.ofNullable(noiseSettings);
    }

    public boolean generateNoise() {
        return generateNoise;
    }

    public boolean generateSurface() {
        return generateSurface;
    }

    public boolean usesVanillaNoisePipeline() {
        return mode == GenerationMode.VANILLA || generateNoise || generateSurface || inheritVanillaBiomeCarvers();
    }

    public boolean generateStructures() {
        return generateStructures;
    }

    public boolean generateDecorations() {
        return generateDecorations;
    }

    public boolean generateCarvers() {
        return generateCarvers;
    }

    public boolean spawnOriginalMobs() {
        return spawnOriginalMobs;
    }

    public EnumSet<DecorationStep> decorationSteps() {
        return decorationSteps.clone();
    }

    /**
     * Structure set keys allowed to generate. Empty means all available sets.
     */
    public Set<Key> includedStructureSets() {
        return includedStructureSets;
    }

    /**
     * Structure set keys prevented from generating.
     */
    public Set<Key> excludedStructureSets() {
        return excludedStructureSets;
    }

    public boolean structureSetEnabled(Key structureSet) {
        Objects.requireNonNull(structureSet, "structureSet");
        return generateStructures
                && (includedStructureSets.isEmpty() || includedStructureSets.contains(structureSet))
                && !excludedStructureSets.contains(structureSet);
    }

    public Optional<Key> dimensionType() {
        return Optional.ofNullable(dimensionType);
    }

    public int seaLevel() {
        return seaLevel;
    }

    public int minY() {
        return minY;
    }

    public int height() {
        return height;
    }

    public int maxY() {
        return minY + height - 1;
    }

    public Optional<Integer> spawnHeight() {
        return Optional.ofNullable(spawnHeight);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {

        private GenerationMode mode = GenerationMode.CUSTOM;
        private BiomeProvider biomeProvider = BiomeProvider.fixed(BiomeKey.PLAINS);
        private VanillaBiomeSource biomeSource = VanillaBiomeSource.CUSTOM;
        private VanillaBiomeGeneration vanillaBiomeGeneration = VanillaBiomeGeneration.NONE;
        private @Nullable Key noiseSettings;
        private boolean generateNoise;
        private boolean generateSurface;
        private boolean generateStructures;
        private boolean generateDecorations;
        private boolean generateCarvers;
        private boolean spawnOriginalMobs;
        private EnumSet<DecorationStep> decorationSteps = EnumSet.allOf(DecorationStep.class);
        private Set<Key> includedStructureSets = Set.of();
        private Set<Key> excludedStructureSets = Set.of();
        private @Nullable Key dimensionType;
        private int seaLevel = 63;
        private int minY = -64;
        private int height = 384;
        private @Nullable Integer spawnHeight;

        private Builder() {
        }

        private Builder(WorldGeneratorSettings settings) {
            this.mode = settings.mode;
            this.biomeProvider = settings.biomeProvider;
            this.biomeSource = settings.biomeSource;
            this.vanillaBiomeGeneration = settings.vanillaBiomeGeneration;
            this.noiseSettings = settings.noiseSettings;
            this.generateNoise = settings.generateNoise;
            this.generateSurface = settings.generateSurface;
            this.generateStructures = settings.generateStructures;
            this.generateDecorations = settings.generateDecorations;
            this.generateCarvers = settings.generateCarvers;
            this.spawnOriginalMobs = settings.spawnOriginalMobs;
            this.decorationSteps = settings.decorationSteps.clone();
            this.includedStructureSets = settings.includedStructureSets;
            this.excludedStructureSets = settings.excludedStructureSets;
            this.dimensionType = settings.dimensionType;
            this.seaLevel = settings.seaLevel;
            this.minY = settings.minY;
            this.height = settings.height;
            this.spawnHeight = settings.spawnHeight;
        }

        public Builder mode(GenerationMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        public Builder biomeProvider(BiomeProvider biomeProvider) {
            this.biomeProvider = Objects.requireNonNull(biomeProvider, "biomeProvider");
            return this;
        }

        public Builder biomeSource(VanillaBiomeSource biomeSource) {
            this.biomeSource = Objects.requireNonNull(biomeSource, "biomeSource");
            return this;
        }

        public Builder fixedBiome(VanillaKey biome) {
            return fixedBiome(Objects.requireNonNull(biome, "biome").key());
        }

        public Builder fixedBiome(Key biome) {
            this.biomeProvider = BiomeProvider.fixed(biome);
            return this;
        }

        public Builder vanillaBiomeGeneration(VanillaBiomeGeneration vanillaBiomeGeneration) {
            this.vanillaBiomeGeneration = Objects.requireNonNull(vanillaBiomeGeneration, "vanillaBiomeGeneration");
            return this;
        }

        public Builder noiseSettings(@Nullable Key noiseSettings) {
            this.noiseSettings = noiseSettings;
            return this;
        }

        public Builder noiseSettings(VanillaKey noiseSettings) {
            this.noiseSettings = Objects.requireNonNull(noiseSettings, "noiseSettings").key();
            return this;
        }

        public Builder generateNoise(boolean generateNoise) {
            this.generateNoise = generateNoise;
            return this;
        }

        public Builder generateSurface(boolean generateSurface) {
            this.generateSurface = generateSurface;
            return this;
        }

        public Builder generateStructures(boolean generateStructures) {
            this.generateStructures = generateStructures;
            return this;
        }

        public Builder generateDecorations(boolean generateDecorations) {
            this.generateDecorations = generateDecorations;
            return this;
        }

        public Builder generateCarvers(boolean generateCarvers) {
            this.generateCarvers = generateCarvers;
            return this;
        }

        public Builder spawnOriginalMobs(boolean spawnOriginalMobs) {
            this.spawnOriginalMobs = spawnOriginalMobs;
            return this;
        }

        public Builder decorationSteps(EnumSet<DecorationStep> decorationSteps) {
            this.decorationSteps = Objects.requireNonNull(decorationSteps, "decorationSteps").clone();
            return this;
        }

        public Builder includedStructureSets(Set<Key> structureSets) {
            this.includedStructureSets = copyKeys(structureSets, "structureSets");
            return this;
        }

        public Builder includeStructureSets(VanillaKey... structureSets) {
            return includedStructureSets(keys(structureSets));
        }

        public Builder includeStructureSets(Key... structureSets) {
            return includedStructureSets(keys(structureSets));
        }

        public Builder excludedStructureSets(Set<Key> structureSets) {
            this.excludedStructureSets = copyKeys(structureSets, "structureSets");
            return this;
        }

        public Builder excludeStructureSets(VanillaKey... structureSets) {
            return excludedStructureSets(keys(structureSets));
        }

        public Builder excludeStructureSets(Key... structureSets) {
            return excludedStructureSets(keys(structureSets));
        }

        public Builder dimensionType(@Nullable Key dimensionType) {
            this.dimensionType = dimensionType;
            return this;
        }

        public Builder dimensionType(VanillaKey dimensionType) {
            this.dimensionType = Objects.requireNonNull(dimensionType, "dimensionType").key();
            return this;
        }

        public Builder seaLevel(int seaLevel) {
            this.seaLevel = seaLevel;
            return this;
        }

        public Builder minY(int minY) {
            this.minY = minY;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder worldHeight(int minY, int height) {
            this.minY = minY;
            this.height = height;
            return this;
        }

        public Builder spawnHeight(@Nullable Integer spawnHeight) {
            this.spawnHeight = spawnHeight;
            return this;
        }

        public WorldGeneratorSettings build() {
            return new WorldGeneratorSettings(
                    mode,
                    biomeProvider,
                    biomeSource,
                    vanillaBiomeGeneration,
                    noiseSettings,
                    generateNoise,
                    generateSurface,
                    generateStructures,
                    generateDecorations,
                    generateCarvers,
                    spawnOriginalMobs,
                    decorationSteps,
                    includedStructureSets,
                    excludedStructureSets,
                    dimensionType,
                    seaLevel,
                    minY,
                    height,
                    spawnHeight);
        }

        private static Set<Key> copyKeys(Set<Key> keys, String name) {
            Objects.requireNonNull(keys, name);
            var copy = new LinkedHashSet<Key>();
            for (var key : keys) {
                copy.add(Objects.requireNonNull(key, name + " cannot contain null"));
            }
            return Set.copyOf(copy);
        }

        private static Set<Key> keys(VanillaKey... keys) {
            Objects.requireNonNull(keys, "keys");
            var result = new LinkedHashSet<Key>();
            for (var key : keys) {
                result.add(Objects.requireNonNull(key, "keys cannot contain null").key());
            }
            return Set.copyOf(result);
        }

        private static Set<Key> keys(Key... keys) {
            Objects.requireNonNull(keys, "keys");
            var result = new LinkedHashSet<Key>();
            for (var key : keys) {
                result.add(Objects.requireNonNull(key, "keys cannot contain null"));
            }
            return Set.copyOf(result);
        }
    }
}
