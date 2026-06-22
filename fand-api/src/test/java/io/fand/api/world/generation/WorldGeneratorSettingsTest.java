package io.fand.api.world.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.world.BiomeKey;
import java.util.List;
import java.util.EnumSet;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class WorldGeneratorSettingsTest {

    @Test
    void builderStoresWorldGenerationControls() {
        var settings = WorldGeneratorSettings.builder()
                .fixedBiome(BiomeKey.DESERT)
                .biomeSource(VanillaBiomeSource.TEMPLATE)
                .vanillaBiomeGeneration(VanillaBiomeGeneration.FEATURES_AND_CARVERS)
                .noiseSettings(NoiseSettingsKey.AMPLIFIED)
                .generateNoise(true)
                .generateSurface(true)
                .generateStructures(true)
                .generateDecorations(true)
                .generateCarvers(true)
                .spawnOriginalMobs(true)
                .decorationSteps(EnumSet.of(DecorationStep.UNDERGROUND_ORES, DecorationStep.VEGETAL_DECORATION))
                .includeStructureSets(StructureSetKey.STRONGHOLDS)
                .excludeStructureSets(StructureSetKey.VILLAGES)
                .dimensionType(DimensionTypeKey.OVERWORLD_CAVES)
                .worldHeight(-32, 128)
                .seaLevel(24)
                .spawnHeight(48)
                .build();

        assertThat(settings.biomeProvider().biomeAt(0, 0, 0)).isEqualTo(BiomeKey.DESERT.key());
        assertThat(settings.biomeSource()).isEqualTo(VanillaBiomeSource.TEMPLATE);
        assertThat(settings.inheritVanillaBiomeFeatures()).isTrue();
        assertThat(settings.inheritVanillaBiomeCarvers()).isTrue();
        assertThat(settings.noiseSettings()).contains(NoiseSettingsKey.AMPLIFIED.key());
        assertThat(settings.generateNoise()).isTrue();
        assertThat(settings.generateSurface()).isTrue();
        assertThat(settings.usesVanillaNoisePipeline()).isTrue();
        assertThat(settings.generateStructures()).isTrue();
        assertThat(settings.generateDecorations()).isTrue();
        assertThat(settings.generateCarvers()).isTrue();
        assertThat(settings.spawnOriginalMobs()).isTrue();
        assertThat(settings.decorationSteps()).containsExactlyInAnyOrder(
                DecorationStep.UNDERGROUND_ORES,
                DecorationStep.VEGETAL_DECORATION);
        assertThat(settings.structureSetEnabled(StructureSetKey.STRONGHOLDS.key())).isTrue();
        assertThat(settings.structureSetEnabled(StructureSetKey.VILLAGES.key())).isFalse();
        assertThat(settings.structureSetEnabled(StructureSetKey.MINESHAFTS.key())).isFalse();
        assertThat(settings.dimensionType()).contains(DimensionTypeKey.OVERWORLD_CAVES.key());
        assertThat(settings.minY()).isEqualTo(-32);
        assertThat(settings.height()).isEqualTo(128);
        assertThat(settings.maxY()).isEqualTo(95);
        assertThat(settings.seaLevel()).isEqualTo(24);
        assertThat(settings.spawnHeight()).contains(48);
    }

    @Test
    void structureSetFilterAllowsAllWhenIncludeSetIsEmpty() {
        var settings = WorldGeneratorSettings.builder()
                .generateStructures(true)
                .excludeStructureSets(StructureSetKey.VILLAGES)
                .build();

        assertThat(settings.structureSetEnabled(StructureSetKey.STRONGHOLDS.key())).isTrue();
        assertThat(settings.structureSetEnabled(StructureSetKey.VILLAGES.key())).isFalse();
    }

    @Test
    void vanillaPresetKeepsCompleteVanillaPipeline() {
        var settings = WorldGeneratorSettings.vanilla();

        assertThat(settings.mode()).isEqualTo(GenerationMode.VANILLA);
        assertThat(settings.biomeSource()).isEqualTo(VanillaBiomeSource.TEMPLATE);
        assertThat(settings.inheritVanillaBiomeFeatures()).isTrue();
        assertThat(settings.inheritVanillaBiomeCarvers()).isTrue();
        assertThat(settings.generateNoise()).isTrue();
        assertThat(settings.generateSurface()).isTrue();
        assertThat(settings.generateStructures()).isTrue();
        assertThat(settings.spawnOriginalMobs()).isTrue();
        assertThat(settings.generateDecorations()).isFalse();
        assertThat(settings.generateCarvers()).isFalse();
    }

    @Test
    void vanillaCarversRequireVanillaNoisePipeline() {
        var settings = WorldGeneratorSettings.builder()
                .vanillaBiomeGeneration(VanillaBiomeGeneration.CARVERS)
                .build();

        assertThat(settings.usesVanillaNoisePipeline()).isTrue();
        assertThat(settings.inheritVanillaBiomeFeatures()).isFalse();
        assertThat(settings.inheritVanillaBiomeCarvers()).isTrue();
    }

    @Test
    void builderRejectsInvalidHeight() {
        assertThatThrownBy(() -> WorldGeneratorSettings.builder().height(17).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("height");
        assertThatThrownBy(() -> WorldGeneratorSettings.builder().spawnHeight(512).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spawnHeight");
    }

    @Test
    void builderRejectsSectionMisalignedWorldHeight() {
        assertThatThrownBy(() -> WorldGeneratorSettings.builder().worldHeight(1, 16).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minY");
    }

    @Test
    void builderRejectsWorldHeightOutsideMinecraftBounds() {
        assertThatThrownBy(() -> WorldGeneratorSettings.builder().worldHeight(-2048, 16).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("world height");
        assertThatThrownBy(() -> WorldGeneratorSettings.builder().worldHeight(0, 4080).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("world height");
        assertThatThrownBy(() -> WorldGeneratorSettings.builder().worldHeight(Integer.MAX_VALUE - 15, 16).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("world height");
    }

    @Test
    void biomeProviderCanDeclareMultiplePossibleBiomes() {
        var settings = WorldGeneratorSettings.builder()
                .biomeProvider(new BiomeProvider() {
                    @Override
                    public Key biomeAt(int quartX, int quartY, int quartZ) {
                        return quartX < 0 ? BiomeKey.DESERT.key() : BiomeKey.PLAINS.key();
                    }

                    @Override
                    public java.util.List<Key> possibleBiomes() {
                        return java.util.List.of(BiomeKey.DESERT.key(), BiomeKey.PLAINS.key());
                    }
                })
                .build();

        assertThat(settings.biomeProvider().biomeAt(-1, 0, 0)).isEqualTo(BiomeKey.DESERT.key());
        assertThat(settings.biomeProvider().possibleBiomes())
                .containsExactly(BiomeKey.DESERT.key(), BiomeKey.PLAINS.key());
    }

    @Test
    void biomeProviderCanDeclareCustomBiomeDefinitions() {
        var biome = CustomBiomeDefinition.builder(Key.key("demo:violet_grove"))
                .temperature(0.7F)
                .downfall(0.9F)
                .colors(new BiomeColors(0x334455, 0x2255AA, 0x112244, 0x77AAFF, 0x55AA55, 0x66BB66))
                .addFeature(DecorationStep.VEGETAL_DECORATION, Key.key("demo:violet_tree"))
                .build();
        BiomeProvider provider = new BiomeProvider() {
            @Override
            public Key biomeAt(int quartX, int quartY, int quartZ) {
                return biome.key();
            }

            @Override
            public List<Key> possibleBiomes() {
                return List.of(biome.key());
            }

            @Override
            public List<CustomBiomeDefinition> customBiomes() {
                return List.of(biome);
            }
        };

        assertThat(provider.customBiomes()).containsExactly(biome);
        assertThat(provider.customBiomes().getFirst().features())
                .containsExactly(new BiomeFeatureReference(DecorationStep.VEGETAL_DECORATION, Key.key("demo:violet_tree")));
    }
}
