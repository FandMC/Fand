package io.fand.datagenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class FandDataGenerator {

    private static final String DEFAULT_NAMESPACE = "minecraft";
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^\\s*(?:public\\s+)?(?:static\\s+)?(?:final\\s+)?(.+?)\\s+([A-Z][A-Z0-9_]*)\\s*=",
            Pattern.MULTILINE);
    private static final Pattern STRING_REGISTER_PATTERN = Pattern.compile(
            "\\bregister\\w*\\s*\\(\\s*\"([^\"]+)\"");
    private static final Pattern REGISTER_HOLDER_PATTERN = Pattern.compile(
            "\\bregisterForHolder\\s*\\(\\s*\"([^\"]+)\"");
    private static final Pattern KEY_REFERENCE_PATTERN = Pattern.compile(
            "\\b(?:BlockIds|ItemIds)\\.([A-Z][A-Z0-9_]*)\\b");
    private static final Pattern CREATE_KEY_PATTERN = Pattern.compile(
            "\\bcreateKey\\s*\\(\\s*\"([^\"]+)\"");
    private static final Pattern STRING_KEY_ARGUMENT_PATTERN = Pattern.compile(
            "\\b(?:register\\w*|createKey|createId|create|registryKey|key)\\s*\\(\\s*\"([^\"]+)\"");
    private static final Pattern SIMPLE_KEY_PATTERN = Pattern.compile(
            "\\bkey\\s*\\(\\s*\"([^\"]+)\"");
    private static final Pattern DEFAULT_NAMESPACE_IDENTIFIER_PATTERN = Pattern.compile(
            "\\bIdentifier\\.withDefaultNamespace\\s*\\(\\s*\"([^\"]+)\"");
    private static final Pattern TEMPERATURE_VARIANT_PATTERN = Pattern.compile(
            "\\bTemperatureVariants\\.([A-Z][A-Z0-9_]*)\\b");
    private static final Pattern SOUND_SET_PATTERN = Pattern.compile(
            "([A-Z][A-Z0-9_]*)\\s*\\(\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\s*\\)");
    private static final Map<String, String> TEMPERATURE_VARIANTS = Map.of(
            "TEMPERATE", "temperate",
            "WARM", "warm",
            "COLD", "cold");
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

    private FandDataGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: FandDataGenerator <minecraft-source-root> <output-source-root>");
        }

        var minecraftSources = Path.of(args[0]);
        var outputSources = Path.of(args[1]);
        var context = new GeneratorContext(minecraftSources);

        writeEnum(
                outputSources,
                "io.fand.api.block",
                "BlockKey",
                "Generated vanilla block registry keys.",
                context.blockKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item",
                "ItemKey",
                "Generated vanilla item registry keys.",
                context.itemKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "EnchantmentKey",
                "Generated vanilla enchantment registry keys.",
                context.enchantmentKeys());
        writeEnum(
                outputSources,
                "io.fand.api.entity",
                "EntityKey",
                "Generated vanilla entity type registry keys.",
                context.entityKeys());
        writeEnum(
                outputSources,
                "io.fand.api.block",
                "BlockEntityKey",
                "Generated vanilla block entity type registry keys.",
                context.blockEntityKeys());
        writeEnum(
                outputSources,
                "io.fand.api.world",
                "FluidKey",
                "Generated vanilla fluid registry keys.",
                context.fluidKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "EffectKey",
                "Generated vanilla mob effect registry keys.",
                context.effectKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "PotionKey",
                "Generated vanilla potion registry keys.",
                context.potionKeys());
        writeEnum(
                outputSources,
                "io.fand.api.entity",
                "AttributeKey",
                "Generated vanilla attribute registry keys.",
                context.attributeKeys());
        writeEnum(
                outputSources,
                "io.fand.api.world",
                "GameEventKey",
                "Generated vanilla game event registry keys.",
                context.gameEventKeys());
        writeEnum(
                outputSources,
                "io.fand.api.world",
                "BiomeKey",
                "Generated vanilla biome registry keys.",
                context.biomeKeys());
        writeEnum(
                outputSources,
                "io.fand.api.world",
                "DamageTypeKey",
                "Generated vanilla damage type registry keys.",
                context.damageTypeKeys());
        writeEnum(
                outputSources,
                "io.fand.api.world.particle",
                "ParticleKey",
                "Generated vanilla particle registry keys.",
                context.particleKeys());
        writeEnum(
                outputSources,
                "io.fand.api.world.sound",
                "SoundKey",
                "Generated vanilla sound event registry keys.",
                context.soundKeys());
        writeEnum(
                outputSources,
                "io.fand.api.world.sound",
                "JukeboxSongKey",
                "Generated vanilla jukebox song registry keys.",
                context.jukeboxSongKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "InstrumentKey",
                "Generated vanilla instrument registry keys.",
                context.instrumentKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "EquipmentAssetKey",
                "Generated vanilla equipment asset registry keys.",
                context.equipmentAssetKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "TrimMaterialKey",
                "Generated vanilla trim material registry keys.",
                context.trimMaterialKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "TrimPatternKey",
                "Generated vanilla trim pattern registry keys.",
                context.trimPatternKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "BannerPatternKey",
                "Generated vanilla banner pattern registry keys.",
                context.bannerPatternKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "DecoratedPotPatternKey",
                "Generated vanilla decorated pot pattern registry keys.",
                context.decoratedPotPatternKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "VillagerVariantKey",
                "Generated vanilla villager variant registry keys.",
                context.villagerVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.entity",
                "VillagerProfessionKey",
                "Generated vanilla villager profession registry keys.",
                context.villagerProfessionKeys());
        writeEnum(
                outputSources,
                "io.fand.api.world",
                "PoiTypeKey",
                "Generated vanilla point of interest type registry keys.",
                context.poiTypeKeys());
        writeEnum(
                outputSources,
                "io.fand.api.inventory",
                "MenuTypeKey",
                "Generated vanilla menu type registry keys.",
                context.menuTypeKeys());
        writeEnum(
                outputSources,
                "io.fand.api.recipe",
                "RecipeTypeKey",
                "Generated vanilla recipe type registry keys.",
                context.recipeTypeKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "WolfVariantKey",
                "Generated vanilla wolf variant registry keys.",
                context.wolfVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "WolfSoundVariantKey",
                "Generated vanilla wolf sound variant registry keys.",
                context.wolfSoundVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "PigVariantKey",
                "Generated vanilla pig variant registry keys.",
                context.pigVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "PigSoundVariantKey",
                "Generated vanilla pig sound variant registry keys.",
                context.pigSoundVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "CowVariantKey",
                "Generated vanilla cow variant registry keys.",
                context.cowVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "CowSoundVariantKey",
                "Generated vanilla cow sound variant registry keys.",
                context.cowSoundVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "ChickenVariantKey",
                "Generated vanilla chicken variant registry keys.",
                context.chickenVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "ChickenSoundVariantKey",
                "Generated vanilla chicken sound variant registry keys.",
                context.chickenSoundVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "CatVariantKey",
                "Generated vanilla cat variant registry keys.",
                context.catVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "CatSoundVariantKey",
                "Generated vanilla cat sound variant registry keys.",
                context.catSoundVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "FrogVariantKey",
                "Generated vanilla frog variant registry keys.",
                context.frogVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "PaintingVariantKey",
                "Generated vanilla painting variant registry keys.",
                context.paintingVariantKeys());
        writeEnum(
                outputSources,
                "io.fand.api.item.component",
                "ZombieNautilusVariantKey",
                "Generated vanilla zombie nautilus variant registry keys.",
                context.zombieNautilusVariantKeys());
    }

    private static void writeEnum(
            Path outputSources,
            String packageName,
            String typeName,
            String javadoc,
            List<KeyEntry> entries) throws IOException {
        if (entries.isEmpty()) {
            throw new IllegalStateException("No entries generated for " + typeName);
        }

        var packagePath = Path.of(packageName.replace('.', '/'));
        var outputFile = outputSources.resolve(packagePath).resolve(typeName + ".java");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, enumSource(packageName, typeName, javadoc, entries), StandardCharsets.UTF_8);
    }

    private static String enumSource(String packageName, String typeName, String javadoc, List<KeyEntry> entries) {
        var source = new StringBuilder();
        source.append("package ").append(packageName).append(";\n\n");
        source.append("import io.fand.api.VanillaKey;\n");
        source.append("import net.kyori.adventure.key.Key;\n\n");
        source.append("/** ").append(javadoc).append(" */\n");
        source.append("public enum ").append(typeName).append(" implements VanillaKey {\n\n");
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            source.append("    ").append(entry.name()).append("(\"").append(entry.key()).append("\")");
            source.append(i == entries.size() - 1 ? ";\n\n" : ",\n");
        }
        source.append("    private final Key key;\n\n");
        source.append("    ").append(typeName).append("(String key) {\n");
        source.append("        this.key = Key.key(key);\n");
        source.append("    }\n\n");
        source.append("    public Key key() {\n");
        source.append("        return key;\n");
        source.append("    }\n\n");
        source.append("    public String asString() {\n");
        source.append("        return key.asString();\n");
        source.append("    }\n\n");
        source.append("    @Override\n");
        source.append("    public String toString() {\n");
        source.append("        return asString();\n");
        source.append("    }\n");
        source.append("}\n");
        return source.toString();
    }

    private record KeyEntry(String name, String key) {

        private KeyEntry {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(key, "key");
        }
    }

    private record StaticField(String type, String name, String initializer) {
    }

    private static final class GeneratorContext {

        private final Path minecraftSources;
        private final Map<String, String> blockIds;
        private final Map<String, String> itemIds;
        private final Map<String, String> blockKeys;

        private GeneratorContext(Path minecraftSources) throws IOException {
            this.minecraftSources = minecraftSources;
            this.blockIds = resourceKeys("net/minecraft/references/BlockIds.java", "Block");
            this.itemIds = resourceKeys("net/minecraft/references/ItemIds.java", "Item");
            this.blockKeys = entriesByName(blockKeys());
        }

        private List<KeyEntry> blockKeys() throws IOException {
            var entries = new EntryCollector();
            for (var field : staticFields("net/minecraft/world/level/block/Blocks.java")) {
                if (!field.type().contains("Block")) {
                    continue;
                }
                firstStringRegisterId(field.initializer())
                        .or(() -> firstReferencedKey(field.initializer(), blockIds))
                        .map(FandDataGenerator::vanillaKey)
                        .ifPresent(key -> entries.add(field.name(), key));
            }
            return entries.sorted();
        }

        private List<KeyEntry> itemKeys() throws IOException {
            var entries = new EntryCollector();
            for (var field : staticFields("net/minecraft/world/item/Items.java")) {
                if (!field.type().equals("Item")) {
                    continue;
                }

                var key = firstStringRegisterId(field.initializer())
                        .map(FandDataGenerator::vanillaKey)
                        .or(() -> firstReferencedKey(field.initializer(), itemIds))
                        .or(() -> referencedBlockName(field.initializer()).map(blockKeys::get))
                        .orElse(vanillaKey(enumNameToPath(field.name())));
                entries.add(field.name(), key);
            }
            return entries.sorted();
        }

        private List<KeyEntry> enchantmentKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/item/enchantment/Enchantments.java", "ResourceKey<Enchantment>");
        }

        private List<KeyEntry> entityKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/EntityType.java", "EntityType<");
        }

        private List<KeyEntry> blockEntityKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/level/block/entity/BlockEntityType.java", "BlockEntityType<");
        }

        private List<KeyEntry> fluidKeys() throws IOException {
            return registryEntries("net/minecraft/world/level/material/Fluids.java", "Fluid");
        }

        private List<KeyEntry> effectKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/effect/MobEffects.java", "MobEffect");
        }

        private List<KeyEntry> potionKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/item/alchemy/Potions.java", "Potion");
        }

        private List<KeyEntry> attributeKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/ai/attributes/Attributes.java", "Attribute");
        }

        private List<KeyEntry> gameEventKeys() throws IOException {
            return registryEntries("net/minecraft/world/level/gameevent/GameEvent.java", "GameEvent");
        }

        private List<KeyEntry> biomeKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/level/biome/Biomes.java", "ResourceKey<Biome>");
        }

        private List<KeyEntry> damageTypeKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/damagesource/DamageTypes.java", "ResourceKey<DamageType>");
        }

        private List<KeyEntry> jukeboxSongKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/item/JukeboxSongs.java", "ResourceKey<JukeboxSong>");
        }

        private List<KeyEntry> instrumentKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/item/Instruments.java", "ResourceKey<Instrument>");
        }

        private List<KeyEntry> equipmentAssetKeys() throws IOException {
            var entries = new EntryCollector();
            for (var field : staticFields("net/minecraft/world/item/equipment/EquipmentAssets.java")) {
                if (!field.type().contains("ResourceKey<EquipmentAsset>")) {
                    continue;
                }
                firstRegistryKeyId(field.initializer())
                        .map(FandDataGenerator::vanillaKey)
                        .ifPresent(key -> entries.add(field.name(), key));
            }

            var source = read("net/minecraft/world/item/equipment/EquipmentAssets.java");
            var matcher = Pattern.compile("createId\\s*\\(\\s*color\\.getSerializedName\\(\\)\\s*\\+\\s*\"_([^\"]+)\"\\s*\\)")
                    .matcher(source);
            while (matcher.find()) {
                for (var color : DYE_COLORS) {
                    var path = color + "_" + matcher.group(1);
                    entries.add(keyToEnumName(path), vanillaKey(path));
                }
            }
            return entries.sorted();
        }

        private List<KeyEntry> trimMaterialKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/item/equipment/trim/TrimMaterials.java", "ResourceKey<TrimMaterial>");
        }

        private List<KeyEntry> trimPatternKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/item/equipment/trim/TrimPatterns.java", "ResourceKey<TrimPattern>");
        }

        private List<KeyEntry> bannerPatternKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/level/block/entity/BannerPatterns.java", "ResourceKey<BannerPattern>");
        }

        private List<KeyEntry> decoratedPotPatternKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/level/block/entity/DecoratedPotPatterns.java", "ResourceKey<DecoratedPotPattern>");
        }

        private List<KeyEntry> villagerVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/npc/villager/VillagerType.java", "ResourceKey<VillagerType>");
        }

        private List<KeyEntry> villagerProfessionKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/npc/villager/VillagerProfession.java", "ResourceKey<VillagerProfession>");
        }

        private List<KeyEntry> poiTypeKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/ai/village/poi/PoiTypes.java", "ResourceKey<PoiType>");
        }

        private List<KeyEntry> menuTypeKeys() throws IOException {
            return registryEntries("net/minecraft/world/inventory/MenuType.java", "MenuType<");
        }

        private List<KeyEntry> recipeTypeKeys() throws IOException {
            return registryEntries("net/minecraft/world/item/crafting/RecipeType.java", "RecipeType<");
        }

        private List<KeyEntry> wolfVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/wolf/WolfVariants.java", "ResourceKey<WolfVariant>");
        }

        private List<KeyEntry> wolfSoundVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/wolf/WolfSoundVariants.java", "ResourceKey<WolfSoundVariant>");
        }

        private List<KeyEntry> pigVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/pig/PigVariants.java", "ResourceKey<PigVariant>");
        }

        private List<KeyEntry> pigSoundVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/pig/PigSoundVariants.java", "ResourceKey<PigSoundVariant>");
        }

        private List<KeyEntry> cowVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/cow/CowVariants.java", "ResourceKey<CowVariant>");
        }

        private List<KeyEntry> cowSoundVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/cow/CowSoundVariants.java", "ResourceKey<CowSoundVariant>");
        }

        private List<KeyEntry> chickenVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/chicken/ChickenVariants.java", "ResourceKey<ChickenVariant>");
        }

        private List<KeyEntry> chickenSoundVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/chicken/ChickenSoundVariants.java", "ResourceKey<ChickenSoundVariant>");
        }

        private List<KeyEntry> catVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/feline/CatVariants.java", "ResourceKey<CatVariant>");
        }

        private List<KeyEntry> catSoundVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/feline/CatSoundVariants.java", "ResourceKey<CatSoundVariant>");
        }

        private List<KeyEntry> frogVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/frog/FrogVariants.java", "ResourceKey<FrogVariant>");
        }

        private List<KeyEntry> paintingVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/decoration/painting/PaintingVariants.java", "ResourceKey<PaintingVariant>");
        }

        private List<KeyEntry> zombieNautilusVariantKeys() throws IOException {
            return registryKeyFields("net/minecraft/world/entity/animal/nautilus/ZombieNautilusVariants.java", "ResourceKey<ZombieNautilusVariant>");
        }

        private List<KeyEntry> particleKeys() throws IOException {
            var entries = new EntryCollector();
            for (var field : staticFields("net/minecraft/core/particles/ParticleTypes.java")) {
                if (!field.type().contains("ParticleType")) {
                    continue;
                }
                firstStringRegisterId(field.initializer())
                        .map(FandDataGenerator::vanillaKey)
                        .ifPresent(key -> entries.add(field.name(), key));
            }
            return entries.sorted();
        }

        private List<KeyEntry> soundKeys() throws IOException {
            var entries = new EntryCollector();
            for (var field : staticFields("net/minecraft/sounds/SoundEvents.java")) {
                if (!field.type().contains("SoundEvent")) {
                    continue;
                }
                firstSoundRegisterId(field.initializer())
                        .map(FandDataGenerator::vanillaKey)
                        .ifPresent(key -> entries.add(field.name(), key));
            }

            for (int i = 0; i < 8; i++) {
                entries.add("GOAT_HORN_SOUND_" + i, vanillaKey("item.goat_horn.sound." + i));
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

        private List<KeyEntry> registryEntries(String relativePath, String typeNeedle) throws IOException {
            var entries = new EntryCollector();
            for (var field : staticFields(relativePath)) {
                if (!field.type().contains(typeNeedle)) {
                    continue;
                }
                firstStringRegisterId(field.initializer())
                        .or(() -> firstRegistryKeyId(field.initializer()))
                        .map(FandDataGenerator::vanillaKey)
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
                firstRegistryKeyId(field.initializer())
                        .or(() -> firstSoundSetKey(field.initializer()))
                        .map(FandDataGenerator::vanillaKey)
                        .ifPresent(key -> entries.add(field.name(), key));
            }
            return entries.sorted();
        }

        private Map<String, String> resourceKeys(String relativePath, String registryType) throws IOException {
            var keys = new LinkedHashMap<String, String>();
            for (var field : staticFields(relativePath)) {
                if (!field.type().contains("ResourceKey<" + registryType + ">")) {
                    continue;
                }
                firstCreateKey(field.initializer())
                        .map(FandDataGenerator::vanillaKey)
                        .ifPresent(key -> keys.put(field.name(), key));
            }
            return keys;
        }

        private List<StaticField> staticFields(String relativePath) throws IOException {
            var source = read(relativePath);
            var fields = new ArrayList<StaticField>();
            var matcher = FIELD_PATTERN.matcher(source);
            int searchStart = 0;
            while (matcher.find(searchStart)) {
                var type = matcher.group(1).replace('\n', ' ').replace('\r', ' ').trim();
                var name = matcher.group(2);
                int initializerStart = matcher.end();
                int initializerEnd = findInitializerEnd(source, initializerStart);
                fields.add(new StaticField(type, name, source.substring(initializerStart, initializerEnd).trim()));
                searchStart = initializerEnd + 1;
            }
            return fields;
        }

        private String read(String relativePath) throws IOException {
            return Files.readString(minecraftSources.resolve(relativePath), StandardCharsets.UTF_8);
        }

        private void addAnimalSoundVariants(EntryCollector entries, String relativePath, List<String> suffixes) throws IOException {
            var source = read(relativePath);
            var matcher = SOUND_SET_PATTERN.matcher(source);
            while (matcher.find()) {
                var soundEventIdentifier = matcher.group(3);
                for (var suffix : suffixes) {
                    var path = "entity." + soundEventIdentifier + "." + suffix;
                    entries.add(keyToEnumName(soundEventIdentifier + "." + suffix), vanillaKey(path));
                }
            }
        }
    }

    private static Map<String, String> entriesByName(List<KeyEntry> entries) {
        var map = new LinkedHashMap<String, String>();
        for (var entry : entries) {
            map.put(entry.name(), entry.key());
        }
        return map;
    }

    private static java.util.Optional<String> firstStringRegisterId(String initializer) {
        var matcher = STRING_REGISTER_PATTERN.matcher(initializer);
        return matcher.find() ? java.util.Optional.of(matcher.group(1)) : java.util.Optional.empty();
    }

    private static java.util.Optional<String> firstSoundRegisterId(String initializer) {
        var holder = REGISTER_HOLDER_PATTERN.matcher(initializer);
        if (holder.find()) {
            return java.util.Optional.of(holder.group(1));
        }
        return firstStringRegisterId(initializer);
    }

    private static java.util.Optional<String> firstCreateKey(String initializer) {
        var matcher = CREATE_KEY_PATTERN.matcher(initializer);
        return matcher.find() ? java.util.Optional.of(matcher.group(1)) : java.util.Optional.empty();
    }

    private static java.util.Optional<String> firstSimpleKey(String initializer) {
        var matcher = SIMPLE_KEY_PATTERN.matcher(initializer);
        return matcher.find() ? java.util.Optional.of(matcher.group(1)) : java.util.Optional.empty();
    }

    private static java.util.Optional<String> firstRegistryKeyId(String initializer) {
        var stringKey = STRING_KEY_ARGUMENT_PATTERN.matcher(initializer);
        if (stringKey.find()) {
            return java.util.Optional.of(stringKey.group(1));
        }

        var identifier = DEFAULT_NAMESPACE_IDENTIFIER_PATTERN.matcher(initializer);
        if (identifier.find()) {
            return java.util.Optional.of(identifier.group(1));
        }

        var temperatureVariant = TEMPERATURE_VARIANT_PATTERN.matcher(initializer);
        if (temperatureVariant.find()) {
            return java.util.Optional.ofNullable(TEMPERATURE_VARIANTS.get(temperatureVariant.group(1)));
        }

        return java.util.Optional.empty();
    }

    private static java.util.Optional<String> firstSoundSetKey(String initializer) {
        var matcher = Pattern.compile("\\bSoundSet\\.([A-Z][A-Z0-9_]*)\\b").matcher(initializer);
        return matcher.find()
                ? java.util.Optional.of(enumNameToPath(matcher.group(1)))
                : java.util.Optional.empty();
    }

    private static java.util.Optional<String> firstReferencedKey(String initializer, Map<String, String> keys) {
        var matcher = KEY_REFERENCE_PATTERN.matcher(initializer);
        while (matcher.find()) {
            var key = keys.get(matcher.group(1));
            if (key != null) {
                return java.util.Optional.of(key);
            }
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<String> referencedBlockName(String initializer) {
        var matcher = Pattern.compile("\\bBlocks\\.([A-Z][A-Z0-9_]*)\\b").matcher(initializer);
        return matcher.find() ? java.util.Optional.of(matcher.group(1)) : java.util.Optional.empty();
    }

    private static int findInitializerEnd(String source, int start) {
        int parenDepth = 0;
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int i = start; i < source.length(); i++) {
            char c = source.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && (inString || inChar)) {
                escaped = true;
                continue;
            }
            if (c == '"' && !inChar) {
                inString = !inString;
                continue;
            }
            if (c == '\'' && !inString) {
                inChar = !inChar;
                continue;
            }
            if (inString || inChar) {
                continue;
            }
            switch (c) {
                case '(' -> parenDepth++;
                case ')' -> parenDepth--;
                case '{' -> braceDepth++;
                case '}' -> braceDepth--;
                case '[' -> bracketDepth++;
                case ']' -> bracketDepth--;
                case ';' -> {
                    if (parenDepth == 0 && braceDepth == 0 && bracketDepth == 0) {
                        return i;
                    }
                }
                default -> {
                }
            }
        }
        throw new IllegalStateException("Unterminated static field initializer");
    }

    private static String vanillaKey(String path) {
        return path.contains(":") ? path : DEFAULT_NAMESPACE + ":" + path;
    }

    private static String enumNameToPath(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static String keyToEnumName(String key) {
        var path = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        var name = path.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        name = name.replaceAll("^_+", "").replaceAll("_+$", "");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Cannot create enum name from key: " + key);
        }
        if (Character.isDigit(name.charAt(0))) {
            return "_" + name;
        }
        return name;
    }

    private static final class EntryCollector {

        private final Map<String, String> entries = new LinkedHashMap<>();
        private final Set<String> keys = new LinkedHashSet<>();

        private void add(String requestedName, String key) {
            if (!keys.add(key)) {
                return;
            }
            var name = uniqueName(requestedName, key);
            entries.put(name, key);
        }

        private String uniqueName(String requestedName, String key) {
            var candidate = requestedName;
            int suffix = 2;
            while (entries.containsKey(candidate) && !Objects.equals(entries.get(candidate), key)) {
                candidate = requestedName + "_" + suffix;
                suffix++;
            }
            return candidate;
        }

        private List<KeyEntry> sorted() {
            return entries.entrySet().stream()
                    .map(entry -> new KeyEntry(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(KeyEntry::name))
                    .toList();
        }
    }
}
