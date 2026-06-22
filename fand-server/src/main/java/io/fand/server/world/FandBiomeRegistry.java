package io.fand.server.world;

import io.fand.api.world.generation.BiomeColors;
import io.fand.api.world.generation.CustomBiomeDefinition;
import io.fand.api.world.generation.DecorationStep;
import io.fand.api.world.generation.WorldGeneratorSettings;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class FandBiomeRegistry {

    private static final int DEFAULT_WATER_COLOR = 0x3F76E4;

    private FandBiomeRegistry() {
    }

    public static void applyCustomBiomes(MinecraftServer server, WorldGeneratorSettings settings) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(settings, "settings");
        var definitions = settings.biomeProvider().customBiomes();
        if (definitions.isEmpty()) {
            return;
        }
        var registry = server.registryAccess().lookupOrThrow(Registries.BIOME);
        if (!(registry instanceof MappedRegistry<Biome> mapped)) {
            throw new IllegalStateException("Biome registry is not writable: " + registry);
        }
        for (var definition : definitions) {
            mapped.fand$registerRuntime(
                    ResourceKey.create(Registries.BIOME, identifier(definition.key())),
                    biome(server, definition),
                    RegistrationInfo.BUILT_IN);
        }
    }

    private static Biome biome(MinecraftServer server, CustomBiomeDefinition definition) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(definition.precipitation() != CustomBiomeDefinition.Precipitation.NONE)
                .temperature(definition.temperature())
                .temperatureAdjustment(definition.precipitation() == CustomBiomeDefinition.Precipitation.SNOW
                        ? Biome.TemperatureModifier.FROZEN
                        : Biome.TemperatureModifier.NONE)
                .downfall(definition.downfall())
                .specialEffects(effects(definition.colors()))
                .generationSettings(generationSettings(server, definition))
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .build();
    }

    private static BiomeSpecialEffects effects(BiomeColors colors) {
        var builder = new BiomeSpecialEffects.Builder()
                .waterColor(colors.waterColor().orElse(DEFAULT_WATER_COLOR));
        colors.foliageColor().ifPresent(builder::foliageColorOverride);
        colors.grassColor().ifPresent(builder::grassColorOverride);
        return builder.build();
    }

    private static BiomeGenerationSettings generationSettings(MinecraftServer server, CustomBiomeDefinition definition) {
        var features = server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        var builder = new BiomeGenerationSettings.PlainBuilder();
        for (var feature : definition.features()) {
            Holder<PlacedFeature> holder = features.get(ResourceKey.create(Registries.PLACED_FEATURE, identifier(feature.placedFeature())))
                    .orElseThrow(() -> new IllegalArgumentException("Placed feature is not available: " + feature.placedFeature().asString()));
            builder.addFeature(step(feature.step()), holder);
        }
        return builder.build();
    }

    private static GenerationStep.Decoration step(DecorationStep step) {
        return switch (step) {
            case RAW_GENERATION -> GenerationStep.Decoration.RAW_GENERATION;
            case LAKES -> GenerationStep.Decoration.LAKES;
            case LOCAL_MODIFICATIONS -> GenerationStep.Decoration.LOCAL_MODIFICATIONS;
            case UNDERGROUND_STRUCTURES -> GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
            case SURFACE_STRUCTURES -> GenerationStep.Decoration.SURFACE_STRUCTURES;
            case STRONGHOLDS -> GenerationStep.Decoration.STRONGHOLDS;
            case UNDERGROUND_ORES -> GenerationStep.Decoration.UNDERGROUND_ORES;
            case UNDERGROUND_DECORATION -> GenerationStep.Decoration.UNDERGROUND_DECORATION;
            case FLUID_SPRINGS -> GenerationStep.Decoration.FLUID_SPRINGS;
            case VEGETAL_DECORATION -> GenerationStep.Decoration.VEGETAL_DECORATION;
            case TOP_LAYER_MODIFICATION -> GenerationStep.Decoration.TOP_LAYER_MODIFICATION;
        };
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }
}
