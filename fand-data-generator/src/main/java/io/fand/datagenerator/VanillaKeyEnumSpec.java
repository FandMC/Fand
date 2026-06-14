package io.fand.datagenerator;

import java.io.IOException;
import java.util.List;

record VanillaKeyEnumSpec(
        String packageName,
        String typeName,
        String javadoc,
        EntryProvider provider
) {

    List<KeyEntry> entries(VanillaRegistrySources sources) throws IOException {
        return provider.generate(sources);
    }

    static List<VanillaKeyEnumSpec> all() {
        return List.of(
                spec("io.fand.api.block", "BlockKey", "Generated vanilla block registry keys.", VanillaRegistrySources::blockKeys),
                spec("io.fand.api.item", "ItemKey", "Generated vanilla item registry keys.", VanillaRegistrySources::itemKeys),
                spec("io.fand.api.item.component", "EnchantmentKey", "Generated vanilla enchantment registry keys.", VanillaRegistrySources::enchantmentKeys),
                spec("io.fand.api.entity", "EntityKey", "Generated vanilla entity type registry keys.", VanillaRegistrySources::entityKeys),
                spec("io.fand.api.block", "BlockTagKey", "Generated vanilla block tag keys.", VanillaRegistrySources::blockTagKeys),
                spec("io.fand.api.item", "ItemTagKey", "Generated vanilla item tag keys.", VanillaRegistrySources::itemTagKeys),
                spec("io.fand.api.entity", "EntityTypeTagKey", "Generated vanilla entity type tag keys.", VanillaRegistrySources::entityTypeTagKeys),
                spec("io.fand.api.block", "BlockEntityKey", "Generated vanilla block entity type registry keys.", VanillaRegistrySources::blockEntityKeys),
                spec("io.fand.api.world", "FluidKey", "Generated vanilla fluid registry keys.", VanillaRegistrySources::fluidKeys),
                spec("io.fand.api.item.component", "EffectKey", "Generated vanilla mob effect registry keys.", VanillaRegistrySources::effectKeys),
                spec("io.fand.api.item.component", "PotionKey", "Generated vanilla potion registry keys.", VanillaRegistrySources::potionKeys),
                spec("io.fand.api.entity", "AttributeKey", "Generated vanilla attribute registry keys.", VanillaRegistrySources::attributeKeys),
                spec("io.fand.api.world", "GameEventKey", "Generated vanilla game event registry keys.", VanillaRegistrySources::gameEventKeys),
                spec("io.fand.api.world", "GameRuleKey", "Generated vanilla game rule keys.", VanillaRegistrySources::gameRuleKeys),
                spec("io.fand.api.world", "BiomeKey", "Generated vanilla biome registry keys.", VanillaRegistrySources::biomeKeys),
                spec("io.fand.api.world.generation", "ConfiguredFeatureKey", "Generated vanilla configured feature registry keys.", VanillaRegistrySources::configuredFeatureKeys),
                spec("io.fand.api.world.generation", "PlacedFeatureKey", "Generated vanilla placed feature registry keys.", VanillaRegistrySources::placedFeatureKeys),
                spec("io.fand.api.world.generation", "ConfiguredCarverKey", "Generated vanilla configured carver registry keys.", VanillaRegistrySources::configuredCarverKeys),
                spec("io.fand.api.world.generation", "StructureKey", "Generated vanilla structure registry keys.", VanillaRegistrySources::structureKeys),
                spec("io.fand.api.world.generation", "StructureSetKey", "Generated vanilla structure set registry keys.", VanillaRegistrySources::structureSetKeys),
                spec("io.fand.api.world.generation", "DimensionTypeKey", "Generated vanilla dimension type registry keys.", VanillaRegistrySources::dimensionTypeKeys),
                spec("io.fand.api.world.generation", "NoiseSettingsKey", "Generated vanilla noise settings registry keys.", VanillaRegistrySources::noiseSettingsKeys),
                spec("io.fand.api.world.generation", "DensityFunctionKey", "Generated vanilla density function registry keys.", VanillaRegistrySources::densityFunctionKeys),
                spec("io.fand.api.world.generation", "TemplatePoolKey", "Generated vanilla structure template pool registry keys.", VanillaRegistrySources::templatePoolKeys),
                spec("io.fand.api.world.generation", "StructureProcessorListKey", "Generated vanilla structure processor list registry keys.", VanillaRegistrySources::structureProcessorListKeys),
                spec("io.fand.api.world.generation", "FlatLevelPresetKey", "Generated vanilla flat level preset registry keys.", VanillaRegistrySources::flatLevelPresetKeys),
                spec("io.fand.api.world.generation", "MultiNoiseBiomeSourcePresetKey", "Generated vanilla multi-noise biome source preset registry keys.", VanillaRegistrySources::multiNoiseBiomeSourcePresetKeys),
                spec("io.fand.api.world.generation", "NoiseKey", "Generated vanilla noise parameter registry keys.", VanillaRegistrySources::noiseKeys),
                spec("io.fand.api.world.generation", "WorldPresetKey", "Generated vanilla world preset registry keys.", VanillaRegistrySources::worldPresetKeys),
                spec("io.fand.api.world.generation", "BiomeSourceTypeKey", "Generated vanilla biome source type registry keys.", VanillaRegistrySources::biomeSourceTypeKeys),
                spec("io.fand.api.world.generation", "ChunkGeneratorTypeKey", "Generated vanilla chunk generator type registry keys.", VanillaRegistrySources::chunkGeneratorTypeKeys),
                spec("io.fand.api.world.generation", "FeatureTypeKey", "Generated vanilla feature type registry keys.", VanillaRegistrySources::featureTypeKeys),
                spec("io.fand.api.world.generation", "CarverTypeKey", "Generated vanilla carver type registry keys.", VanillaRegistrySources::carverTypeKeys),
                spec("io.fand.api.world.generation", "PlacementModifierTypeKey", "Generated vanilla placement modifier type registry keys.", VanillaRegistrySources::placementModifierTypeKeys),
                spec("io.fand.api.world.generation", "StructureTypeKey", "Generated vanilla structure type registry keys.", VanillaRegistrySources::structureTypeKeys),
                spec("io.fand.api.world.generation", "StructurePlacementTypeKey", "Generated vanilla structure placement type registry keys.", VanillaRegistrySources::structurePlacementTypeKeys),
                spec("io.fand.api.world.generation", "StructurePoolElementTypeKey", "Generated vanilla structure pool element type registry keys.", VanillaRegistrySources::structurePoolElementTypeKeys),
                spec("io.fand.api.world.generation", "PoolAliasBindingTypeKey", "Generated vanilla pool alias binding type registry keys.", VanillaRegistrySources::poolAliasBindingTypeKeys),
                spec("io.fand.api.world.generation", "StructureProcessorTypeKey", "Generated vanilla structure processor type registry keys.", VanillaRegistrySources::structureProcessorTypeKeys),
                spec("io.fand.api.world.generation", "MaterialConditionTypeKey", "Generated vanilla material condition type registry keys.", VanillaRegistrySources::materialConditionTypeKeys),
                spec("io.fand.api.world.generation", "MaterialRuleTypeKey", "Generated vanilla material rule type registry keys.", VanillaRegistrySources::materialRuleTypeKeys),
                spec("io.fand.api.world", "DamageTypeKey", "Generated vanilla damage type registry keys.", VanillaRegistrySources::damageTypeKeys),
                spec("io.fand.api.world.particle", "ParticleKey", "Generated vanilla particle registry keys.", VanillaRegistrySources::particleKeys),
                spec("io.fand.api.world.sound", "SoundKey", "Generated vanilla sound event registry keys.", VanillaRegistrySources::soundKeys),
                spec("io.fand.api.world.sound", "JukeboxSongKey", "Generated vanilla jukebox song registry keys.", VanillaRegistrySources::jukeboxSongKeys),
                spec("io.fand.api.item.component", "InstrumentKey", "Generated vanilla instrument registry keys.", VanillaRegistrySources::instrumentKeys),
                spec("io.fand.api.item.component", "EquipmentAssetKey", "Generated vanilla equipment asset registry keys.", VanillaRegistrySources::equipmentAssetKeys),
                spec("io.fand.api.item.component", "TrimMaterialKey", "Generated vanilla trim material registry keys.", VanillaRegistrySources::trimMaterialKeys),
                spec("io.fand.api.item.component", "TrimPatternKey", "Generated vanilla trim pattern registry keys.", VanillaRegistrySources::trimPatternKeys),
                spec("io.fand.api.item.component", "BannerPatternKey", "Generated vanilla banner pattern registry keys.", VanillaRegistrySources::bannerPatternKeys),
                spec("io.fand.api.item.component", "DecoratedPotPatternKey", "Generated vanilla decorated pot pattern registry keys.", VanillaRegistrySources::decoratedPotPatternKeys),
                spec("io.fand.api.item.component", "VillagerVariantKey", "Generated vanilla villager variant registry keys.", VanillaRegistrySources::villagerVariantKeys),
                spec("io.fand.api.entity", "VillagerProfessionKey", "Generated vanilla villager profession registry keys.", VanillaRegistrySources::villagerProfessionKeys),
                spec("io.fand.api.world", "PoiTypeKey", "Generated vanilla point of interest type registry keys.", VanillaRegistrySources::poiTypeKeys),
                spec("io.fand.api.inventory", "MenuTypeKey", "Generated vanilla menu type registry keys.", VanillaRegistrySources::menuTypeKeys),
                spec("io.fand.api.recipe", "RecipeTypeKey", "Generated vanilla recipe type registry keys.", VanillaRegistrySources::recipeTypeKeys),
                spec("io.fand.api.player", "StatisticKey", "Generated vanilla custom statistic keys.", VanillaRegistrySources::statisticKeys),
                spec("io.fand.api.item.component", "WolfVariantKey", "Generated vanilla wolf variant registry keys.", VanillaRegistrySources::wolfVariantKeys),
                spec("io.fand.api.item.component", "WolfSoundVariantKey", "Generated vanilla wolf sound variant registry keys.", VanillaRegistrySources::wolfSoundVariantKeys),
                spec("io.fand.api.item.component", "PigVariantKey", "Generated vanilla pig variant registry keys.", VanillaRegistrySources::pigVariantKeys),
                spec("io.fand.api.item.component", "PigSoundVariantKey", "Generated vanilla pig sound variant registry keys.", VanillaRegistrySources::pigSoundVariantKeys),
                spec("io.fand.api.item.component", "CowVariantKey", "Generated vanilla cow variant registry keys.", VanillaRegistrySources::cowVariantKeys),
                spec("io.fand.api.item.component", "CowSoundVariantKey", "Generated vanilla cow sound variant registry keys.", VanillaRegistrySources::cowSoundVariantKeys),
                spec("io.fand.api.item.component", "ChickenVariantKey", "Generated vanilla chicken variant registry keys.", VanillaRegistrySources::chickenVariantKeys),
                spec("io.fand.api.item.component", "ChickenSoundVariantKey", "Generated vanilla chicken sound variant registry keys.", VanillaRegistrySources::chickenSoundVariantKeys),
                spec("io.fand.api.item.component", "CatVariantKey", "Generated vanilla cat variant registry keys.", VanillaRegistrySources::catVariantKeys),
                spec("io.fand.api.item.component", "CatSoundVariantKey", "Generated vanilla cat sound variant registry keys.", VanillaRegistrySources::catSoundVariantKeys),
                spec("io.fand.api.item.component", "FrogVariantKey", "Generated vanilla frog variant registry keys.", VanillaRegistrySources::frogVariantKeys),
                spec("io.fand.api.item.component", "PaintingVariantKey", "Generated vanilla painting variant registry keys.", VanillaRegistrySources::paintingVariantKeys),
                spec("io.fand.api.item.component", "ZombieNautilusVariantKey", "Generated vanilla zombie nautilus variant registry keys.", VanillaRegistrySources::zombieNautilusVariantKeys)
        );
    }

    private static VanillaKeyEnumSpec spec(
            String packageName,
            String typeName,
            String javadoc,
            EntryProvider entries) {
        return new VanillaKeyEnumSpec(packageName, typeName, javadoc, entries);
    }

    @FunctionalInterface
    interface EntryProvider {
        List<KeyEntry> generate(VanillaRegistrySources sources) throws IOException;
    }
}
