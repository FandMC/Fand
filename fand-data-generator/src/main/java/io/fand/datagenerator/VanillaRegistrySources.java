package io.fand.datagenerator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class VanillaRegistrySources {

    private static final List<String> DYE_COLORS = List.of(
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black");

    private final MinecraftSourceSet sources;
    private final Map<String, String> blockIds;
    private final Map<String, String> itemIds;
    private final Map<String, String> blockKeys;

    VanillaRegistrySources(MinecraftSourceSet sources) throws IOException {
        this.sources = sources;
        this.blockIds = resourceKeys("net/minecraft/references/BlockIds.java", "Block");
        this.itemIds = resourceKeys("net/minecraft/references/ItemIds.java", "Item");
        this.blockKeys = KeyNames.entriesByName(blockKeys());
    }

    List<KeyEntry> blockKeys() throws IOException {
        var entries = new EntryCollector();
        for (var field : staticFields("net/minecraft/world/level/block/Blocks.java")) {
            if (!field.type().contains("Block")) {
                continue;
            }
            KeyExtractors.firstStringRegisterId(field.initializer())
                    .or(() -> KeyExtractors.firstReferencedKey(field.initializer(), blockIds))
                    .map(KeyNames::vanillaKey)
                    .ifPresent(key -> entries.add(field.name(), key));
        }
        return entries.sorted();
    }

    List<KeyEntry> itemKeys() throws IOException {
        var entries = new EntryCollector();
        for (var field : staticFields("net/minecraft/world/item/Items.java")) {
            if (!field.type().equals("Item")) {
                continue;
            }

            var key = KeyExtractors.firstStringRegisterId(field.initializer())
                    .map(KeyNames::vanillaKey)
                    .or(() -> KeyExtractors.firstReferencedKey(field.initializer(), itemIds))
                    .or(() -> KeyExtractors.referencedBlockName(field.initializer()).map(blockKeys::get))
                    .orElse(KeyNames.vanillaKey(KeyNames.enumNameToPath(field.name())));
            entries.add(field.name(), key);
        }
        return entries.sorted();
    }

    List<KeyEntry> enchantmentKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/item/enchantment/Enchantments.java", "ResourceKey<Enchantment>");
    }

    List<KeyEntry> entityKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/EntityType.java", "EntityType<");
    }

    List<KeyEntry> blockEntityKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/level/block/entity/BlockEntityType.java", "BlockEntityType<");
    }

    List<KeyEntry> fluidKeys() throws IOException {
        return registryEntries("net/minecraft/world/level/material/Fluids.java", "Fluid");
    }

    List<KeyEntry> effectKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/effect/MobEffects.java", "MobEffect");
    }

    List<KeyEntry> potionKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/item/alchemy/Potions.java", "Potion");
    }

    List<KeyEntry> attributeKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/ai/attributes/Attributes.java", "Attribute");
    }

    List<KeyEntry> gameEventKeys() throws IOException {
        return registryEntries("net/minecraft/world/level/gameevent/GameEvent.java", "GameEvent");
    }

    List<KeyEntry> gameRuleKeys() throws IOException {
        return registryEntries("net/minecraft/world/level/gamerules/GameRules.java", "GameRule<");
    }

    List<KeyEntry> biomeKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/level/biome/Biomes.java", "ResourceKey<Biome>");
    }

    List<KeyEntry> configuredFeatureKeys() throws IOException {
        return registryKeyFieldsInFiles("net/minecraft/data/worldgen/features", "ResourceKey<ConfiguredFeature", true);
    }

    List<KeyEntry> placedFeatureKeys() throws IOException {
        return registryKeyFieldsInFiles("net/minecraft/data/worldgen/placement", "ResourceKey<PlacedFeature>", true);
    }

    List<KeyEntry> configuredCarverKeys() throws IOException {
        return registryKeyFields("net/minecraft/data/worldgen/Carvers.java", "ResourceKey<ConfiguredWorldCarver");
    }

    List<KeyEntry> structureKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/level/levelgen/structure/BuiltinStructures.java", "ResourceKey<Structure>");
    }

    List<KeyEntry> structureSetKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/level/levelgen/structure/BuiltinStructureSets.java", "ResourceKey<StructureSet>");
    }

    List<KeyEntry> dimensionTypeKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/level/dimension/BuiltinDimensionTypes.java", "ResourceKey<DimensionType>");
    }

    List<KeyEntry> noiseSettingsKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/level/levelgen/NoiseGeneratorSettings.java", "ResourceKey<NoiseGeneratorSettings>");
    }

    List<KeyEntry> densityFunctionKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/level/levelgen/NoiseRouterData.java", "ResourceKey<DensityFunction>");
    }

    List<KeyEntry> templatePoolKeys() throws IOException {
        return registryKeyFieldsInFiles("net/minecraft/data/worldgen", "ResourceKey<StructureTemplatePool>", true);
    }

    List<KeyEntry> structureProcessorListKeys() throws IOException {
        return registryKeyFields("net/minecraft/data/worldgen/ProcessorLists.java", "ResourceKey<StructureProcessorList>");
    }

    List<KeyEntry> flatLevelPresetKeys() throws IOException {
        return registryKeyFields(
                "net/minecraft/world/level/levelgen/flat/FlatLevelGeneratorPresets.java",
                "ResourceKey<FlatLevelGeneratorPreset>");
    }

    List<KeyEntry> multiNoiseBiomeSourcePresetKeys() throws IOException {
        return registryKeyFields(
                "net/minecraft/world/level/biome/MultiNoiseBiomeSourceParameterLists.java",
                "ResourceKey<MultiNoiseBiomeSourceParameterList>");
    }

    List<KeyEntry> noiseKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/level/levelgen/Noises.java", "ResourceKey<NormalNoise.NoiseParameters>");
    }

    List<KeyEntry> worldPresetKeys() throws IOException {
        return registryKeyFields(
                "net/minecraft/world/level/levelgen/presets/WorldPresets.java",
                "ResourceKey<WorldPreset>");
    }

    List<KeyEntry> biomeSourceTypeKeys() throws IOException {
        return registryCallKeys("net/minecraft/world/level/biome/BiomeSources.java");
    }

    List<KeyEntry> chunkGeneratorTypeKeys() throws IOException {
        return registryCallKeys("net/minecraft/world/level/chunk/ChunkGenerators.java");
    }

    List<KeyEntry> featureTypeKeys() throws IOException {
        return registryCallKeys("net/minecraft/world/level/levelgen/feature/Feature.java");
    }

    List<KeyEntry> carverTypeKeys() throws IOException {
        return registryCallKeys("net/minecraft/world/level/levelgen/carver/WorldCarver.java");
    }

    List<KeyEntry> placementModifierTypeKeys() throws IOException {
        return registryCallKeys("net/minecraft/world/level/levelgen/placement/PlacementModifierType.java");
    }

    List<KeyEntry> structureTypeKeys() throws IOException {
        return registryCallKeys("net/minecraft/world/level/levelgen/structure/StructureType.java");
    }

    List<KeyEntry> structurePlacementTypeKeys() throws IOException {
        return registryCallKeys("net/minecraft/world/level/levelgen/structure/placement/StructurePlacementType.java");
    }

    List<KeyEntry> structurePoolElementTypeKeys() throws IOException {
        return registryCallKeys("net/minecraft/world/level/levelgen/structure/pools/StructurePoolElementType.java");
    }

    List<KeyEntry> poolAliasBindingTypeKeys() throws IOException {
        return registryCallKeys("net/minecraft/world/level/levelgen/structure/pools/alias/PoolAliasBindings.java");
    }

    List<KeyEntry> structureProcessorTypeKeys() throws IOException {
        return registryCallKeys("net/minecraft/world/level/levelgen/structure/templatesystem/StructureProcessorType.java");
    }

    List<KeyEntry> materialConditionTypeKeys() throws IOException {
        return registryCallKeys(
                "net/minecraft/world/level/levelgen/SurfaceRules.java",
                "public interface ConditionSource",
                "protected static final class Context");
    }

    List<KeyEntry> materialRuleTypeKeys() throws IOException {
        return registryCallKeys(
                "net/minecraft/world/level/levelgen/SurfaceRules.java",
                "public interface RuleSource",
                "private record SequenceRule");
    }

    List<KeyEntry> damageTypeKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/damagesource/DamageTypes.java", "ResourceKey<DamageType>");
    }

    List<KeyEntry> particleKeys() throws IOException {
        var entries = new EntryCollector();
        for (var field : staticFields("net/minecraft/core/particles/ParticleTypes.java")) {
            if (!field.type().contains("ParticleType")) {
                continue;
            }
            KeyExtractors.firstStringRegisterId(field.initializer())
                    .map(KeyNames::vanillaKey)
                    .ifPresent(key -> entries.add(field.name(), key));
        }
        return entries.sorted();
    }

    List<KeyEntry> soundKeys() throws IOException {
        var entries = new EntryCollector();
        for (var field : staticFields("net/minecraft/sounds/SoundEvents.java")) {
            if (!field.type().contains("SoundEvent")) {
                continue;
            }
            KeyExtractors.firstSoundRegisterId(field.initializer())
                    .map(KeyNames::vanillaKey)
                    .ifPresent(key -> entries.add(field.name(), key));
        }

        for (int i = 0; i < 8; i++) {
            entries.add("GOAT_HORN_SOUND_" + i, KeyNames.vanillaKey("item.goat_horn.sound." + i));
        }
        addAnimalSoundVariants(
                entries,
                "net/minecraft/world/entity/animal/wolf/WolfSoundVariants.java",
                List.of("ambient", "death", "growl", "hurt", "pant", "whine"));
        addAnimalSoundVariants(
                entries,
                "net/minecraft/world/entity/animal/chicken/ChickenSoundVariants.java",
                List.of("ambient", "hurt", "death"));
        addAnimalSoundVariants(
                entries,
                "net/minecraft/world/entity/animal/cow/CowSoundVariants.java",
                List.of("ambient", "hurt", "death", "step"));
        addAnimalSoundVariants(
                entries,
                "net/minecraft/world/entity/animal/pig/PigSoundVariants.java",
                List.of("ambient", "hurt", "death", "eat"));
        addAnimalSoundVariants(
                entries,
                "net/minecraft/world/entity/animal/feline/CatSoundVariants.java",
                List.of("ambient", "stray_ambient", "hiss", "hurt", "death", "eat", "beg_for_food", "purr", "purreow"));
        return entries.sorted();
    }

    List<KeyEntry> jukeboxSongKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/item/JukeboxSongs.java", "ResourceKey<JukeboxSong>");
    }

    List<KeyEntry> instrumentKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/item/Instruments.java", "ResourceKey<Instrument>");
    }

    List<KeyEntry> equipmentAssetKeys() throws IOException {
        var entries = new EntryCollector();
        for (var field : staticFields("net/minecraft/world/item/equipment/EquipmentAssets.java")) {
            if (!field.type().contains("ResourceKey<EquipmentAsset>")) {
                continue;
            }
            KeyExtractors.firstRegistryKeyId(field.initializer())
                    .map(KeyNames::vanillaKey)
                    .ifPresent(key -> entries.add(field.name(), key));
        }

        var source = read("net/minecraft/world/item/equipment/EquipmentAssets.java");
        var matcher = Pattern.compile("createId\\s*\\(\\s*color\\.getSerializedName\\(\\)\\s*\\+\\s*\"_([^\"]+)\"\\s*\\)")
                .matcher(source);
        while (matcher.find()) {
            for (var color : DYE_COLORS) {
                var path = color + "_" + matcher.group(1);
                entries.add(KeyNames.keyToEnumName(path), KeyNames.vanillaKey(path));
            }
        }
        return entries.sorted();
    }

    List<KeyEntry> trimMaterialKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/item/equipment/trim/TrimMaterials.java", "ResourceKey<TrimMaterial>");
    }

    List<KeyEntry> trimPatternKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/item/equipment/trim/TrimPatterns.java", "ResourceKey<TrimPattern>");
    }

    List<KeyEntry> bannerPatternKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/level/block/entity/BannerPatterns.java", "ResourceKey<BannerPattern>");
    }

    List<KeyEntry> decoratedPotPatternKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/level/block/entity/DecoratedPotPatterns.java", "ResourceKey<DecoratedPotPattern>");
    }

    List<KeyEntry> villagerVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/npc/villager/VillagerType.java", "ResourceKey<VillagerType>");
    }

    List<KeyEntry> villagerProfessionKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/npc/villager/VillagerProfession.java", "ResourceKey<VillagerProfession>");
    }

    List<KeyEntry> poiTypeKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/ai/village/poi/PoiTypes.java", "ResourceKey<PoiType>");
    }

    List<KeyEntry> menuTypeKeys() throws IOException {
        return registryEntries("net/minecraft/world/inventory/MenuType.java", "MenuType<");
    }

    List<KeyEntry> recipeTypeKeys() throws IOException {
        return registryEntries("net/minecraft/world/item/crafting/RecipeType.java", "RecipeType<");
    }

    List<KeyEntry> statisticKeys() throws IOException {
        var entries = new EntryCollector();
        for (var field : staticFields("net/minecraft/stats/Stats.java")) {
            if (!field.type().equals("Identifier")) {
                continue;
            }
            KeyExtractors.firstCustomStatId(field.initializer())
                    .map(KeyNames::vanillaKey)
                    .ifPresent(key -> entries.add(field.name(), key));
        }
        return entries.sorted();
    }

    List<KeyEntry> wolfVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/wolf/WolfVariants.java", "ResourceKey<WolfVariant>");
    }

    List<KeyEntry> wolfSoundVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/wolf/WolfSoundVariants.java", "ResourceKey<WolfSoundVariant>");
    }

    List<KeyEntry> pigVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/pig/PigVariants.java", "ResourceKey<PigVariant>");
    }

    List<KeyEntry> pigSoundVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/pig/PigSoundVariants.java", "ResourceKey<PigSoundVariant>");
    }

    List<KeyEntry> cowVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/cow/CowVariants.java", "ResourceKey<CowVariant>");
    }

    List<KeyEntry> cowSoundVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/cow/CowSoundVariants.java", "ResourceKey<CowSoundVariant>");
    }

    List<KeyEntry> chickenVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/chicken/ChickenVariants.java", "ResourceKey<ChickenVariant>");
    }

    List<KeyEntry> chickenSoundVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/chicken/ChickenSoundVariants.java", "ResourceKey<ChickenSoundVariant>");
    }

    List<KeyEntry> catVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/feline/CatVariants.java", "ResourceKey<CatVariant>");
    }

    List<KeyEntry> catSoundVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/feline/CatSoundVariants.java", "ResourceKey<CatSoundVariant>");
    }

    List<KeyEntry> frogVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/frog/FrogVariants.java", "ResourceKey<FrogVariant>");
    }

    List<KeyEntry> paintingVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/decoration/painting/PaintingVariants.java", "ResourceKey<PaintingVariant>");
    }

    List<KeyEntry> zombieNautilusVariantKeys() throws IOException {
        return registryKeyFields("net/minecraft/world/entity/animal/nautilus/ZombieNautilusVariants.java", "ResourceKey<ZombieNautilusVariant>");
    }

    private List<KeyEntry> registryEntries(String relativePath, String typeNeedle) throws IOException {
        var entries = new EntryCollector();
        for (var field : staticFields(relativePath)) {
            if (!field.type().contains(typeNeedle)) {
                continue;
            }
            KeyExtractors.firstStringRegisterId(field.initializer())
                    .or(() -> KeyExtractors.firstRegistryKeyId(field.initializer()))
                    .map(KeyNames::vanillaKey)
                    .ifPresent(key -> entries.add(field.name(), key));
        }
        return entries.sorted();
    }

    private List<KeyEntry> registryKeyFields(String relativePath, String typeNeedle) throws IOException {
        var entries = new EntryCollector();
        for (var field : staticFields(relativePath)) {
            if (!field.type().contains(typeNeedle)) {
                continue;
            }
            KeyExtractors.firstRegistryKeyId(field.initializer())
                    .or(() -> KeyExtractors.firstSoundSetKey(field.initializer()))
                    .map(KeyNames::vanillaKey)
                    .ifPresent(key -> entries.add(field.name(), key));
        }
        return entries.sorted();
    }

    private List<KeyEntry> registryKeyFieldsInFiles(String relativeRoot, String typeNeedle) throws IOException {
        return registryKeyFieldsInFiles(relativeRoot, typeNeedle, false);
    }

    private List<KeyEntry> registryKeyFieldsInFiles(String relativeRoot, String typeNeedle, boolean nameFromKey) throws IOException {
        var entries = new EntryCollector();
        for (var file : sources.files(relativeRoot, ".java")) {
            for (var field : sources.staticFields(sources.relativePath(file))) {
                if (!field.type().contains(typeNeedle)) {
                    continue;
                }
                KeyExtractors.firstRegistryKeyId(field.initializer())
                        .map(KeyNames::vanillaKey)
                        .ifPresent(key -> entries.add(nameFromKey ? KeyNames.keyToEnumName(key) : field.name(), key));
            }
        }
        return entries.sorted();
    }

    private List<KeyEntry> registryCallKeys(String relativePath) throws IOException {
        return registryCallKeys(relativePath, null, null);
    }

    private List<KeyEntry> registryCallKeys(String relativePath, String startNeedle, String stopBefore) throws IOException {
        var entries = new EntryCollector();
        var source = read(relativePath);
        int start = startNeedle == null ? 0 : source.indexOf(startNeedle);
        if (start < 0) {
            throw new IllegalStateException("Cannot find " + startNeedle + " in " + relativePath);
        }
        int end = stopBefore == null ? source.length() : source.indexOf(stopBefore, start);
        if (end < 0) {
            end = source.length();
        }
        var matcher = KeyExtractors.stringKeyMatcher(source.substring(start, end));
        while (matcher.find()) {
            var path = matcher.group(1);
            entries.add(KeyNames.keyToEnumName(path), KeyNames.vanillaKey(path));
        }
        return entries.sorted();
    }

    private Map<String, String> resourceKeys(String relativePath, String registryType) throws IOException {
        var keys = new LinkedHashMap<String, String>();
        for (var field : staticFields(relativePath)) {
            if (!field.type().contains("ResourceKey<" + registryType + ">")) {
                continue;
            }
            KeyExtractors.firstCreateKey(field.initializer())
                    .map(KeyNames::vanillaKey)
                    .ifPresent(key -> keys.put(field.name(), key));
        }
        return keys;
    }

    private List<StaticField> staticFields(String relativePath) throws IOException {
        return sources.staticFields(relativePath);
    }

    private String read(String relativePath) throws IOException {
        return sources.read(relativePath);
    }

    private void addAnimalSoundVariants(EntryCollector entries, String relativePath, List<String> suffixes) throws IOException {
        var matcher = KeyExtractors.soundSetMatcher(read(relativePath));
        while (matcher.find()) {
            var soundEventIdentifier = matcher.group(3);
            for (var suffix : suffixes) {
                var path = "entity." + soundEventIdentifier + "." + suffix;
                entries.add(KeyNames.keyToEnumName(soundEventIdentifier + "." + suffix), KeyNames.vanillaKey(path));
            }
        }
    }
}
