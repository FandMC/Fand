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
                spec("io.fand.api.block", "BlockEntityKey", "Generated vanilla block entity type registry keys.", VanillaRegistrySources::blockEntityKeys),
                spec("io.fand.api.world", "FluidKey", "Generated vanilla fluid registry keys.", VanillaRegistrySources::fluidKeys),
                spec("io.fand.api.item.component", "EffectKey", "Generated vanilla mob effect registry keys.", VanillaRegistrySources::effectKeys),
                spec("io.fand.api.item.component", "PotionKey", "Generated vanilla potion registry keys.", VanillaRegistrySources::potionKeys),
                spec("io.fand.api.entity", "AttributeKey", "Generated vanilla attribute registry keys.", VanillaRegistrySources::attributeKeys),
                spec("io.fand.api.world", "GameEventKey", "Generated vanilla game event registry keys.", VanillaRegistrySources::gameEventKeys),
                spec("io.fand.api.world", "GameRuleKey", "Generated vanilla game rule keys.", VanillaRegistrySources::gameRuleKeys),
                spec("io.fand.api.world", "BiomeKey", "Generated vanilla biome registry keys.", VanillaRegistrySources::biomeKeys),
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
