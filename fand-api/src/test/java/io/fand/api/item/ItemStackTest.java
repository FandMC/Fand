package io.fand.api.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonObject;
import io.fand.api.block.BlockEntityKey;
import io.fand.api.entity.AttributeKey;
import io.fand.api.entity.EntityKey;
import io.fand.api.item.component.CustomModelData;
import io.fand.api.item.component.EffectKey;
import io.fand.api.item.component.EnchantmentKey;
import io.fand.api.item.component.EquipmentAssetKey;
import io.fand.api.item.component.InstrumentKey;
import io.fand.api.item.component.ItemArmorTrim;
import io.fand.api.item.component.ItemAttackRange;
import io.fand.api.item.component.ItemAttributeModifierDisplay;
import io.fand.api.item.component.ItemAttributeModifierOperation;
import io.fand.api.item.component.ItemAttributeModifiers;
import io.fand.api.item.component.ItemBannerPatterns;
import io.fand.api.item.component.ItemBlockStateProperties;
import io.fand.api.item.component.ItemComponentKeys;
import io.fand.api.item.component.ItemComponents;
import io.fand.api.item.component.ItemConsumable;
import io.fand.api.item.component.ItemConsumeEffect;
import io.fand.api.item.component.ItemContainerContents;
import io.fand.api.item.component.ItemContainerLoot;
import io.fand.api.item.component.ItemDyeColor;
import io.fand.api.item.component.ItemEffectInstance;
import io.fand.api.item.component.ItemEntityVariant;
import io.fand.api.item.component.ItemEquipmentSlot;
import io.fand.api.item.component.ItemEquippable;
import io.fand.api.item.component.ItemEquipmentSlotGroup;
import io.fand.api.item.component.ItemFireworkExplosion;
import io.fand.api.item.component.ItemFireworkShape;
import io.fand.api.item.component.ItemFireworks;
import io.fand.api.item.component.ItemFood;
import io.fand.api.item.component.ItemKeySet;
import io.fand.api.item.component.ItemMapDecorations;
import io.fand.api.item.component.ItemMapPostProcessing;
import io.fand.api.item.component.ItemPotionContents;
import io.fand.api.item.component.ItemProfile;
import io.fand.api.item.component.ItemRarity;
import io.fand.api.item.component.ItemSwingAnimation;
import io.fand.api.item.component.ItemSwingAnimationType;
import io.fand.api.item.component.ItemTemplate;
import io.fand.api.item.component.ItemTool;
import io.fand.api.item.component.ItemTypedEntityData;
import io.fand.api.item.component.ItemUseAnimation;
import io.fand.api.item.component.ItemUseCooldown;
import io.fand.api.item.component.ItemUseEffects;
import io.fand.api.item.component.ItemWeapon;
import io.fand.api.item.component.PotionKey;
import io.fand.api.item.component.TrimMaterialKey;
import io.fand.api.item.component.TrimPatternKey;
import io.fand.api.item.component.VillagerVariantKey;
import io.fand.api.item.component.WolfVariantKey;
import io.fand.api.world.DamageTypeKey;
import io.fand.api.world.sound.JukeboxSongKey;
import io.fand.api.world.sound.SoundKey;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class ItemStackTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);

    @Test
    void keepsComponentsImmutable() {
        var stack = new ItemStack(DIAMOND, 1)
                .withCustomName(Component.text("Named"));

        var changed = stack.withoutCustomName();

        assertThat(stack.customName()).contains(Component.text("Named"));
        assertThat(changed.customName()).isEmpty();
    }

    @Test
    void readsAndWritesCommonComponents() {
        var customData = new JsonObject();
        customData.addProperty("owner", "test");

        var stack = new ItemStack(DIAMOND, 1)
                .withCustomName(Component.text("Custom"))
                .withItemName(Component.text("Item"))
                .withLore(Component.text("one"), Component.text("two"))
                .withItemModel(Key.key("fand:test_model"))
                .withCustomModelData(new CustomModelData(List.of(1.0F), List.of(true), List.of("demo"), List.of(0x336699)))
                .withEnchantmentGlintOverride(true)
                .withUnbreakable(true)
                .withDamage(3)
                .withMaxDamage(10)
                .withRepairCost(2)
                .withRarity(ItemRarity.RARE)
                .withEnchantment(EnchantmentKey.SHARPNESS, 4)
                .upgradeEnchantment(EnchantmentKey.SHARPNESS, 5)
                .withStoredEnchantment(EnchantmentKey.MENDING, 1)
                .withEnchantable(30)
                .withMinimumAttackCharge(0.75F)
                .withTooltipHidden(false)
                .withHiddenTooltipComponent(ItemComponentKeys.ENCHANTMENTS, true)
                .withDyedColor(0x336699)
                .withMapColor(0x112233)
                .withTooltipStyle(Key.key("minecraft:fand"))
                .withNoteBlockSound(Key.key("minecraft:block.note_block.pling"))
                .withCustomData(customData);

        assertThat(stack.customName()).contains(Component.text("Custom"));
        assertThat(stack.itemName()).contains(Component.text("Item"));
        assertThat(stack.lore()).containsExactly(Component.text("one"), Component.text("two"));
        assertThat(stack.itemModel()).contains(Key.key("fand:test_model"));
        assertThat(stack.customModelData()).contains(new CustomModelData(List.of(1.0F), List.of(true), List.of("demo"), List.of(0x336699)));
        assertThat(stack.enchantmentGlintOverride()).contains(true);
        assertThat(stack.unbreakable()).isTrue();
        assertThat(stack.damage()).contains(3);
        assertThat(stack.maxDamage()).contains(10);
        assertThat(stack.repairCost()).contains(2);
        assertThat(stack.rarity()).contains(ItemRarity.RARE);
        assertThat(stack.enchantments().level(EnchantmentKey.SHARPNESS)).isEqualTo(5);
        assertThat(stack.storedEnchantments().level(EnchantmentKey.MENDING)).isEqualTo(1);
        assertThat(stack.enchantable()).contains(30);
        assertThat(stack.minimumAttackCharge()).contains(0.75F);
        assertThat(stack.tooltipDisplay().hides(ItemComponentKeys.ENCHANTMENTS)).isTrue();
        assertThat(stack.dyedColor()).contains(0x336699);
        assertThat(stack.mapColor()).contains(0x112233);
        assertThat(stack.tooltipStyle()).contains(Key.key("minecraft:fand"));
        assertThat(stack.noteBlockSound()).contains(Key.key("minecraft:block.note_block.pling"));
        assertThat(stack.customData()).get().extracting(json -> json.get("owner").getAsString()).isEqualTo("test");
    }

    @Test
    void readsAndWritesExpandedComponents() {
        var haste = new ItemEffectInstance(Key.key("minecraft:haste"), 200, 1, false, true, true, Optional.empty());
        var consumeEffects = List.<ItemConsumeEffect>of(
                new ItemConsumeEffect.ApplyEffects(List.of(haste), 0.75F),
                new ItemConsumeEffect.PlaySound(Key.key("minecraft:block.amethyst_block.chime")));
        var attributeModifiers = ItemAttributeModifiers.EMPTY.with(new ItemAttributeModifiers.Entry(
                Key.key("minecraft:generic.attack_damage"),
                Key.key("fand:bonus_damage"),
                3.0D,
                ItemAttributeModifierOperation.ADD_VALUE,
                ItemEquipmentSlotGroup.MAINHAND,
                ItemAttributeModifierDisplay.override(Component.text("Bonus damage"))));
        var tool = new ItemTool(
                List.of(ItemTool.Rule.minesAndDrops(ItemKeySet.tag(Key.key("minecraft:mineable/pickaxe")), 6.0F)),
                1.5F,
                2,
                false);
        var mapDecorations = ItemMapDecorations.EMPTY.with(
                "spawn",
                new ItemMapDecorations.Entry(Key.key("minecraft:red_marker"), 12.5D, -4.25D, 90.0F));
        var potionContents = new ItemPotionContents(
                Optional.of(Key.key("minecraft:strong_healing")),
                Optional.of(0xCC33AA),
                List.of(haste),
                Optional.of("fand_boost"));
        var bannerPatterns = new ItemBannerPatterns(List.of(
                new ItemBannerPatterns.Layer(Key.key("minecraft:stripe_downright"), ItemDyeColor.YELLOW)));
        var profile = new ItemProfile(
                Optional.of("tester"),
                Optional.empty(),
                List.of(ItemProfile.Property.signed("textures", "skin-value", "skin-signature")),
                Optional.of(Key.key("minecraft:entity/player/wide/steve")),
                Optional.empty(),
                Optional.empty(),
                Optional.of("wide"));
        var containerLoot = new ItemContainerLoot(Key.key("minecraft:chests/simple_dungeon"), 7L);
        var fireworkExplosion = new ItemFireworkExplosion(
                ItemFireworkShape.STAR,
                List.of(0xFF0000),
                List.of(0x00FF00),
                true,
                true);
        var fireworks = new ItemFireworks(2, List.of(fireworkExplosion));
        var template = new ItemTemplate(Key.key("minecraft:arrow"), 3, ItemComponents.EMPTY);

        var stack = new ItemStack(DIAMOND, 1)
                .withUseEffects(new ItemUseEffects(true, false, 0.5F))
                .withFood(new ItemFood(4, 2.4F, true))
                .withConsumable(new ItemConsumable(
                        0.8F,
                        ItemUseAnimation.DRINK,
                        Key.key("minecraft:entity.generic.drink"),
                        false,
                        consumeEffects))
                .withUseRemainder(ItemTemplate.of(Key.key("minecraft:bowl")))
                .withUseCooldown(new ItemUseCooldown(1.25F, Optional.of(Key.key("fand:test"))))
                .withAttributeModifiers(attributeModifiers)
                .withTool(tool)
                .withWeapon(new ItemWeapon(2, 5.0F))
                .withAttackRange(new ItemAttackRange(0.0F, 4.0F, 0.0F, 6.0F, 0.2F, 1.25F))
                .withSwingAnimation(new ItemSwingAnimation(ItemSwingAnimationType.STAB, 8))
                .withDye(ItemDyeColor.BLUE)
                .withMapDecorations(mapDecorations)
                .withMapPostProcessing(ItemMapPostProcessing.LOCK)
                .withChargedProjectiles(List.of(template))
                .withBundleContents(List.of(template))
                .withPotionContents(potionContents)
                .withFireworkExplosion(fireworkExplosion)
                .withFireworks(fireworks)
                .withBannerPatterns(bannerPatterns)
                .withContainer(ItemContainerContents.EMPTY.withSlot(0, template))
                .withBlockState(ItemBlockStateProperties.EMPTY.with("facing", "north"))
                .withContainerLoot(containerLoot)
                .withProfile(profile)
                .withFoxVariant(ItemEntityVariant.Fox.SNOW)
                .withTropicalFishPattern(ItemEntityVariant.TropicalFishPattern.KOB)
                .withWolfCollar(ItemDyeColor.RED)
                .withCatCollar(ItemDyeColor.GREEN)
                .withSheepColor(ItemDyeColor.WHITE)
                .withShulkerColor(ItemDyeColor.BLACK);

        assertThat(stack.useEffects()).contains(new ItemUseEffects(true, false, 0.5F));
        assertThat(stack.food()).contains(new ItemFood(4, 2.4F, true));
        assertThat(stack.consumable()).contains(new ItemConsumable(
                0.8F,
                ItemUseAnimation.DRINK,
                Key.key("minecraft:entity.generic.drink"),
                false,
                consumeEffects));
        assertThat(stack.useRemainder()).contains(ItemTemplate.of(Key.key("minecraft:bowl")));
        assertThat(stack.useCooldown()).contains(new ItemUseCooldown(1.25F, Optional.of(Key.key("fand:test"))));
        assertThat(stack.attributeModifiers()).contains(attributeModifiers);
        assertThat(stack.tool()).contains(tool);
        assertThat(stack.weapon()).contains(new ItemWeapon(2, 5.0F));
        assertThat(stack.attackRange()).contains(new ItemAttackRange(0.0F, 4.0F, 0.0F, 6.0F, 0.2F, 1.25F));
        assertThat(stack.swingAnimation()).contains(new ItemSwingAnimation(ItemSwingAnimationType.STAB, 8));
        assertThat(stack.dye()).contains(ItemDyeColor.BLUE);
        assertThat(stack.mapDecorations()).contains(mapDecorations);
        assertThat(stack.mapPostProcessing()).contains(ItemMapPostProcessing.LOCK);
        assertThat(stack.chargedProjectiles()).containsExactly(template);
        assertThat(stack.bundleContents()).containsExactly(template);
        assertThat(stack.potionContents()).contains(potionContents);
        assertThat(stack.fireworkExplosion()).contains(fireworkExplosion);
        assertThat(stack.fireworks()).contains(fireworks);
        assertThat(stack.bannerPatterns()).contains(bannerPatterns);
        assertThat(stack.container()).contains(ItemContainerContents.EMPTY.withSlot(0, template));
        assertThat(stack.blockState()).contains(ItemBlockStateProperties.EMPTY.with("facing", "north"));
        assertThat(stack.containerLoot()).contains(containerLoot);
        assertThat(stack.profile()).contains(profile);
        assertThat(stack.foxVariant()).contains(ItemEntityVariant.Fox.SNOW);
        assertThat(stack.tropicalFishPattern()).contains(ItemEntityVariant.TropicalFishPattern.KOB);
        assertThat(stack.wolfCollar()).contains(ItemDyeColor.RED);
        assertThat(stack.catCollar()).contains(ItemDyeColor.GREEN);
        assertThat(stack.sheepColor()).contains(ItemDyeColor.WHITE);
        assertThat(stack.shulkerColor()).contains(ItemDyeColor.BLACK);
    }

    @Test
    void generatedVanillaKeysCanBuildItemComponents() {
        var entityData = new JsonObject();
        entityData.addProperty("powered", true);
        var blockEntityData = new JsonObject();
        blockEntityData.addProperty("LootTable", "minecraft:chests/simple_dungeon");
        var speed = new ItemEffectInstance(EffectKey.SPEED, 200);
        var consumeEffects = List.<ItemConsumeEffect>of(
                new ItemConsumeEffect.ApplyEffects(List.of(speed)),
                new ItemConsumeEffect.RemoveEffects(ItemKeySet.of(EffectKey.HASTE)),
                new ItemConsumeEffect.PlaySound(SoundKey.AMETHYST_BLOCK_CHIME));
        var attributeModifiers = ItemAttributeModifiers.EMPTY.with(new ItemAttributeModifiers.Entry(
                AttributeKey.ATTACK_DAMAGE,
                Key.key("fand:bonus_damage"),
                3.0D,
                ItemAttributeModifierOperation.ADD_VALUE,
                ItemEquipmentSlotGroup.MAINHAND,
                ItemAttributeModifierDisplay.DEFAULT));
        var equippable = new ItemEquippable(ItemEquipmentSlot.CHEST, SoundKey.ARMOR_EQUIP_DIAMOND)
                .withAssetId(EquipmentAssetKey.DIAMOND)
                .withAllowedEntities(EntityKey.PLAYER, EntityKey.ZOMBIE);
        var template = new ItemTemplate(ItemKey.ARROW, 3, ItemComponents.EMPTY);

        var stack = new ItemStack(DIAMOND, 1)
                .withConsumable(new ItemConsumable(
                        0.8F,
                        ItemUseAnimation.DRINK,
                        SoundKey.GENERIC_DRINK,
                        false,
                        consumeEffects))
                .withUseRemainder(template)
                .withAttributeModifiers(attributeModifiers)
                .withEquippable(equippable)
                .withPotionContents(new ItemPotionContents(PotionKey.WATER))
                .withTrim(new ItemArmorTrim(TrimMaterialKey.DIAMOND, TrimPatternKey.SENTRY))
                .withEntityData(new ItemTypedEntityData(EntityKey.CREEPER, entityData))
                .withBlockEntityData(new ItemTypedEntityData(BlockEntityKey.CHEST, blockEntityData))
                .withDamageType(DamageTypeKey.FALL)
                .withNoteBlockSound(SoundKey.NOTE_BLOCK_BELL)
                .withInstrument(InstrumentKey.PONDER_GOAT_HORN)
                .withProvidesTrimMaterial(TrimMaterialKey.DIAMOND)
                .withJukeboxPlayable(JukeboxSongKey.PIGSTEP)
                .withBreakSound(SoundKey.GLASS_BREAK)
                .withVillagerVariant(VillagerVariantKey.PLAINS)
                .withWolfVariant(WolfVariantKey.SNOWY);

        assertThat(stack.consumable()).contains(new ItemConsumable(
                0.8F,
                ItemUseAnimation.DRINK,
                SoundKey.GENERIC_DRINK.key(),
                false,
                consumeEffects));
        assertThat(stack.useRemainder()).contains(template);
        assertThat(stack.attributeModifiers()).contains(attributeModifiers);
        assertThat(stack.equippable()).contains(equippable);
        assertThat(stack.potionContents()).contains(new ItemPotionContents(PotionKey.WATER));
        assertThat(stack.trim()).contains(new ItemArmorTrim(TrimMaterialKey.DIAMOND, TrimPatternKey.SENTRY));
        assertThat(stack.entityData()).contains(new ItemTypedEntityData(EntityKey.CREEPER, entityData));
        assertThat(stack.blockEntityData()).contains(new ItemTypedEntityData(BlockEntityKey.CHEST, blockEntityData));
        assertThat(stack.damageType()).contains(DamageTypeKey.FALL.key());
        assertThat(stack.noteBlockSound()).contains(SoundKey.NOTE_BLOCK_BELL.key());
        assertThat(stack.instrument()).contains(InstrumentKey.PONDER_GOAT_HORN.key());
        assertThat(stack.providesTrimMaterial()).contains(TrimMaterialKey.DIAMOND.key());
        assertThat(stack.jukeboxPlayable()).contains(JukeboxSongKey.PIGSTEP.key());
        assertThat(stack.breakSound()).contains(SoundKey.GLASS_BREAK.key());
        assertThat(stack.villagerVariant()).contains(VillagerVariantKey.PLAINS.key());
        assertThat(stack.wolfVariant()).contains(WolfVariantKey.SNOWY.key());
    }

    @Test
    void exposesNamedApiForEveryKnownComponentKey() {
        var methodNames = Set.of(ItemStack.class.getDeclaredMethods()).stream()
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(COMPONENT_METHODS.keySet()).containsExactlyInAnyOrderElementsOf(ItemComponentKeys.all());
        COMPONENT_METHODS.forEach((key, methods) -> assertThat(methodNames)
                .as(key.asString())
                .contains(methods));
    }

    @Test
    void maxStackSizeComponentControlsAmountValidation() {
        var stack = new ItemStack(DIAMOND, 1)
                .withMaxStackSize(99)
                .withAmount(99);

        assertThat(stack.amount()).isEqualTo(99);
        assertThat(stack.maxStackSize()).isEqualTo(99);
        assertThatThrownBy(() -> stack.withAmount(100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds max stack size 99");
    }

    @Test
    void applyComponentsMergesValuesAndRemovals() {
        var base = new ItemStack(DIAMOND, 1)
                .withCustomName(Component.text("before"))
                .withLore(Component.text("lore"));
        var patch = ItemComponents.of(ItemComponentKeys.RARITY, new com.google.gson.JsonPrimitive("epic"))
                .remove(ItemComponentKeys.CUSTOM_NAME);

        var changed = base.applyComponents(patch);

        assertThat(changed.customName()).isEmpty();
        assertThat(changed.lore()).containsExactly(Component.text("lore"));
        assertThat(changed.rarity()).contains(ItemRarity.EPIC);
        assertThat(changed.components().removes(ItemComponentKeys.CUSTOM_NAME)).isTrue();
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private static final Map<Key, String[]> COMPONENT_METHODS = Map.ofEntries(
            Map.entry(ItemComponentKeys.CUSTOM_DATA, methods("customData", "withCustomData", "withoutCustomData")),
            Map.entry(ItemComponentKeys.MAX_STACK_SIZE, methods("maxStackSizeOverride", "withMaxStackSize", "withoutMaxStackSize")),
            Map.entry(ItemComponentKeys.MAX_DAMAGE, methods("maxDamage", "withMaxDamage", "withoutMaxDamage")),
            Map.entry(ItemComponentKeys.DAMAGE, methods("damage", "withDamage", "withoutDamage")),
            Map.entry(ItemComponentKeys.UNBREAKABLE, methods("unbreakable", "withUnbreakable", "withoutUnbreakable")),
            Map.entry(ItemComponentKeys.USE_EFFECTS, methods("useEffects", "withUseEffects", "withoutUseEffects")),
            Map.entry(ItemComponentKeys.CUSTOM_NAME, methods("customName", "withCustomName", "withoutCustomName")),
            Map.entry(ItemComponentKeys.MINIMUM_ATTACK_CHARGE, methods("minimumAttackCharge", "withMinimumAttackCharge", "withoutMinimumAttackCharge")),
            Map.entry(ItemComponentKeys.DAMAGE_TYPE, methods("damageType", "withDamageType", "withoutDamageType")),
            Map.entry(ItemComponentKeys.ITEM_NAME, methods("itemName", "withItemName", "withoutItemName")),
            Map.entry(ItemComponentKeys.ITEM_MODEL, methods("itemModel", "withItemModel", "withoutItemModel")),
            Map.entry(ItemComponentKeys.LORE, methods("lore", "withLore", "withoutLore")),
            Map.entry(ItemComponentKeys.RARITY, methods("rarity", "withRarity", "withoutRarity")),
            Map.entry(ItemComponentKeys.ENCHANTMENTS, methods("enchantments", "withEnchantments", "withoutEnchantments")),
            Map.entry(ItemComponentKeys.CAN_PLACE_ON, methods("canPlaceOn", "withCanPlaceOn", "withoutCanPlaceOn")),
            Map.entry(ItemComponentKeys.CAN_BREAK, methods("canBreak", "withCanBreak", "withoutCanBreak")),
            Map.entry(ItemComponentKeys.ATTRIBUTE_MODIFIERS, methods("attributeModifiers", "withAttributeModifiers", "withoutAttributeModifiers")),
            Map.entry(ItemComponentKeys.CUSTOM_MODEL_DATA, methods("customModelData", "withCustomModelData", "withoutCustomModelData")),
            Map.entry(ItemComponentKeys.TOOLTIP_DISPLAY, methods("tooltipDisplay", "withTooltipDisplay", "withoutTooltipDisplay")),
            Map.entry(ItemComponentKeys.REPAIR_COST, methods("repairCost", "withRepairCost", "withoutRepairCost")),
            Map.entry(ItemComponentKeys.CREATIVE_SLOT_LOCK, methods("creativeSlotLock", "withCreativeSlotLock", "withoutCreativeSlotLock")),
            Map.entry(ItemComponentKeys.ENCHANTMENT_GLINT_OVERRIDE, methods("enchantmentGlintOverride", "withEnchantmentGlintOverride", "withoutEnchantmentGlintOverride")),
            Map.entry(ItemComponentKeys.INTANGIBLE_PROJECTILE, methods("intangibleProjectile", "withIntangibleProjectile", "withoutIntangibleProjectile")),
            Map.entry(ItemComponentKeys.FOOD, methods("food", "withFood", "withoutFood")),
            Map.entry(ItemComponentKeys.CONSUMABLE, methods("consumable", "withConsumable", "withoutConsumable")),
            Map.entry(ItemComponentKeys.USE_REMAINDER, methods("useRemainder", "withUseRemainder", "withoutUseRemainder")),
            Map.entry(ItemComponentKeys.USE_COOLDOWN, methods("useCooldown", "withUseCooldown", "withoutUseCooldown")),
            Map.entry(ItemComponentKeys.DAMAGE_RESISTANT, methods("damageResistant", "withDamageResistant", "withoutDamageResistant")),
            Map.entry(ItemComponentKeys.TOOL, methods("tool", "withTool", "withoutTool")),
            Map.entry(ItemComponentKeys.WEAPON, methods("weapon", "withWeapon", "withoutWeapon")),
            Map.entry(ItemComponentKeys.ATTACK_RANGE, methods("attackRange", "withAttackRange", "withoutAttackRange")),
            Map.entry(ItemComponentKeys.ENCHANTABLE, methods("enchantable", "withEnchantable", "withoutEnchantable")),
            Map.entry(ItemComponentKeys.EQUIPPABLE, methods("equippable", "withEquippable", "withoutEquippable")),
            Map.entry(ItemComponentKeys.REPAIRABLE, methods("repairable", "withRepairable", "withoutRepairable")),
            Map.entry(ItemComponentKeys.GLIDER, methods("glider", "withGlider", "withoutGlider")),
            Map.entry(ItemComponentKeys.TOOLTIP_STYLE, methods("tooltipStyle", "withTooltipStyle", "withoutTooltipStyle")),
            Map.entry(ItemComponentKeys.DEATH_PROTECTION, methods("deathProtection", "withDeathProtection", "withoutDeathProtection")),
            Map.entry(ItemComponentKeys.BLOCKS_ATTACKS, methods("blocksAttacks", "withBlocksAttacks", "withoutBlocksAttacks")),
            Map.entry(ItemComponentKeys.PIERCING_WEAPON, methods("piercingWeapon", "withPiercingWeapon", "withoutPiercingWeapon")),
            Map.entry(ItemComponentKeys.KINETIC_WEAPON, methods("kineticWeapon", "withKineticWeapon", "withoutKineticWeapon")),
            Map.entry(ItemComponentKeys.SWING_ANIMATION, methods("swingAnimation", "withSwingAnimation", "withoutSwingAnimation")),
            Map.entry(ItemComponentKeys.ADDITIONAL_TRADE_COST, methods("additionalTradeCost", "withAdditionalTradeCost", "withoutAdditionalTradeCost")),
            Map.entry(ItemComponentKeys.STORED_ENCHANTMENTS, methods("storedEnchantments", "withStoredEnchantments", "withoutStoredEnchantments")),
            Map.entry(ItemComponentKeys.DYE, methods("dye", "withDye", "withoutDye")),
            Map.entry(ItemComponentKeys.DYED_COLOR, methods("dyedColor", "withDyedColor", "withoutDyedColor")),
            Map.entry(ItemComponentKeys.MAP_COLOR, methods("mapColor", "withMapColor", "withoutMapColor")),
            Map.entry(ItemComponentKeys.MAP_ID, methods("mapId", "withMapId", "withoutMapId")),
            Map.entry(ItemComponentKeys.MAP_DECORATIONS, methods("mapDecorations", "withMapDecorations", "withoutMapDecorations")),
            Map.entry(ItemComponentKeys.MAP_POST_PROCESSING, methods("mapPostProcessing", "withMapPostProcessing", "withoutMapPostProcessing")),
            Map.entry(ItemComponentKeys.CHARGED_PROJECTILES, methods("chargedProjectiles", "withChargedProjectiles", "withoutChargedProjectiles")),
            Map.entry(ItemComponentKeys.BUNDLE_CONTENTS, methods("bundleContents", "withBundleContents", "withoutBundleContents")),
            Map.entry(ItemComponentKeys.POTION_CONTENTS, methods("potionContents", "withPotionContents", "withoutPotionContents")),
            Map.entry(ItemComponentKeys.POTION_DURATION_SCALE, methods("potionDurationScale", "withPotionDurationScale", "withoutPotionDurationScale")),
            Map.entry(ItemComponentKeys.SUSPICIOUS_STEW_EFFECTS, methods("suspiciousStewEffects", "withSuspiciousStewEffects", "withoutSuspiciousStewEffects")),
            Map.entry(ItemComponentKeys.WRITABLE_BOOK_CONTENT, methods("writableBookContent", "withWritableBookContent", "withoutWritableBookContent")),
            Map.entry(ItemComponentKeys.WRITTEN_BOOK_CONTENT, methods("writtenBookContent", "withWrittenBookContent", "withoutWrittenBookContent")),
            Map.entry(ItemComponentKeys.TRIM, methods("trim", "withTrim", "withoutTrim")),
            Map.entry(ItemComponentKeys.DEBUG_STICK_STATE, methods("debugStickState", "withDebugStickState", "withoutDebugStickState")),
            Map.entry(ItemComponentKeys.ENTITY_DATA, methods("entityData", "withEntityData", "withoutEntityData")),
            Map.entry(ItemComponentKeys.BUCKET_ENTITY_DATA, methods("bucketEntityData", "withBucketEntityData", "withoutBucketEntityData")),
            Map.entry(ItemComponentKeys.BLOCK_ENTITY_DATA, methods("blockEntityData", "withBlockEntityData", "withoutBlockEntityData")),
            Map.entry(ItemComponentKeys.INSTRUMENT, methods("instrument", "withInstrument", "withoutInstrument")),
            Map.entry(ItemComponentKeys.PROVIDES_TRIM_MATERIAL, methods("providesTrimMaterial", "withProvidesTrimMaterial", "withoutProvidesTrimMaterial")),
            Map.entry(ItemComponentKeys.OMINOUS_BOTTLE_AMPLIFIER, methods("ominousBottleAmplifier", "withOminousBottleAmplifier", "withoutOminousBottleAmplifier")),
            Map.entry(ItemComponentKeys.JUKEBOX_PLAYABLE, methods("jukeboxPlayable", "withJukeboxPlayable", "withoutJukeboxPlayable")),
            Map.entry(ItemComponentKeys.PROVIDES_BANNER_PATTERNS, methods("providesBannerPatterns", "withProvidesBannerPatterns", "withoutProvidesBannerPatterns")),
            Map.entry(ItemComponentKeys.RECIPES, methods("recipes", "withRecipes", "withoutRecipes")),
            Map.entry(ItemComponentKeys.LODESTONE_TRACKER, methods("lodestoneTracker", "withLodestoneTracker", "withoutLodestoneTracker")),
            Map.entry(ItemComponentKeys.FIREWORK_EXPLOSION, methods("fireworkExplosion", "withFireworkExplosion", "withoutFireworkExplosion")),
            Map.entry(ItemComponentKeys.FIREWORKS, methods("fireworks", "withFireworks", "withoutFireworks")),
            Map.entry(ItemComponentKeys.PROFILE, methods("profile", "withProfile", "withoutProfile")),
            Map.entry(ItemComponentKeys.NOTE_BLOCK_SOUND, methods("noteBlockSound", "withNoteBlockSound", "withoutNoteBlockSound")),
            Map.entry(ItemComponentKeys.BANNER_PATTERNS, methods("bannerPatterns", "withBannerPatterns", "withoutBannerPatterns")),
            Map.entry(ItemComponentKeys.BASE_COLOR, methods("baseColor", "withBaseColor", "withoutBaseColor")),
            Map.entry(ItemComponentKeys.POT_DECORATIONS, methods("potDecorations", "withPotDecorations", "withoutPotDecorations")),
            Map.entry(ItemComponentKeys.CONTAINER, methods("container", "withContainer", "withoutContainer")),
            Map.entry(ItemComponentKeys.BLOCK_STATE, methods("blockState", "withBlockState", "withoutBlockState")),
            Map.entry(ItemComponentKeys.BEES, methods("bees", "withBees", "withoutBees")),
            Map.entry(ItemComponentKeys.LOCK, methods("lock", "withLock", "withoutLock")),
            Map.entry(ItemComponentKeys.CONTAINER_LOOT, methods("containerLoot", "withContainerLoot", "withoutContainerLoot")),
            Map.entry(ItemComponentKeys.BREAK_SOUND, methods("breakSound", "withBreakSound", "withoutBreakSound")),
            Map.entry(ItemComponentKeys.VILLAGER_VARIANT, methods("villagerVariant", "withVillagerVariant", "withoutVillagerVariant")),
            Map.entry(ItemComponentKeys.WOLF_VARIANT, methods("wolfVariant", "withWolfVariant", "withoutWolfVariant")),
            Map.entry(ItemComponentKeys.WOLF_SOUND_VARIANT, methods("wolfSoundVariant", "withWolfSoundVariant", "withoutWolfSoundVariant")),
            Map.entry(ItemComponentKeys.WOLF_COLLAR, methods("wolfCollar", "withWolfCollar", "withoutWolfCollar")),
            Map.entry(ItemComponentKeys.FOX_VARIANT, methods("foxVariant", "withFoxVariant", "withoutFoxVariant")),
            Map.entry(ItemComponentKeys.SALMON_SIZE, methods("salmonSize", "withSalmonSize", "withoutSalmonSize")),
            Map.entry(ItemComponentKeys.PARROT_VARIANT, methods("parrotVariant", "withParrotVariant", "withoutParrotVariant")),
            Map.entry(ItemComponentKeys.TROPICAL_FISH_PATTERN, methods("tropicalFishPattern", "withTropicalFishPattern", "withoutTropicalFishPattern")),
            Map.entry(ItemComponentKeys.TROPICAL_FISH_BASE_COLOR, methods("tropicalFishBaseColor", "withTropicalFishBaseColor", "withoutTropicalFishBaseColor")),
            Map.entry(ItemComponentKeys.TROPICAL_FISH_PATTERN_COLOR, methods("tropicalFishPatternColor", "withTropicalFishPatternColor", "withoutTropicalFishPatternColor")),
            Map.entry(ItemComponentKeys.MOOSHROOM_VARIANT, methods("mooshroomVariant", "withMooshroomVariant", "withoutMooshroomVariant")),
            Map.entry(ItemComponentKeys.RABBIT_VARIANT, methods("rabbitVariant", "withRabbitVariant", "withoutRabbitVariant")),
            Map.entry(ItemComponentKeys.PIG_VARIANT, methods("pigVariant", "withPigVariant", "withoutPigVariant")),
            Map.entry(ItemComponentKeys.PIG_SOUND_VARIANT, methods("pigSoundVariant", "withPigSoundVariant", "withoutPigSoundVariant")),
            Map.entry(ItemComponentKeys.COW_VARIANT, methods("cowVariant", "withCowVariant", "withoutCowVariant")),
            Map.entry(ItemComponentKeys.COW_SOUND_VARIANT, methods("cowSoundVariant", "withCowSoundVariant", "withoutCowSoundVariant")),
            Map.entry(ItemComponentKeys.CHICKEN_VARIANT, methods("chickenVariant", "withChickenVariant", "withoutChickenVariant")),
            Map.entry(ItemComponentKeys.CHICKEN_SOUND_VARIANT, methods("chickenSoundVariant", "withChickenSoundVariant", "withoutChickenSoundVariant")),
            Map.entry(ItemComponentKeys.ZOMBIE_NAUTILUS_VARIANT, methods("zombieNautilusVariant", "withZombieNautilusVariant", "withoutZombieNautilusVariant")),
            Map.entry(ItemComponentKeys.FROG_VARIANT, methods("frogVariant", "withFrogVariant", "withoutFrogVariant")),
            Map.entry(ItemComponentKeys.HORSE_VARIANT, methods("horseVariant", "withHorseVariant", "withoutHorseVariant")),
            Map.entry(ItemComponentKeys.PAINTING_VARIANT, methods("paintingVariant", "withPaintingVariant", "withoutPaintingVariant")),
            Map.entry(ItemComponentKeys.LLAMA_VARIANT, methods("llamaVariant", "withLlamaVariant", "withoutLlamaVariant")),
            Map.entry(ItemComponentKeys.AXOLOTL_VARIANT, methods("axolotlVariant", "withAxolotlVariant", "withoutAxolotlVariant")),
            Map.entry(ItemComponentKeys.CAT_VARIANT, methods("catVariant", "withCatVariant", "withoutCatVariant")),
            Map.entry(ItemComponentKeys.CAT_SOUND_VARIANT, methods("catSoundVariant", "withCatSoundVariant", "withoutCatSoundVariant")),
            Map.entry(ItemComponentKeys.CAT_COLLAR, methods("catCollar", "withCatCollar", "withoutCatCollar")),
            Map.entry(ItemComponentKeys.SHEEP_COLOR, methods("sheepColor", "withSheepColor", "withoutSheepColor")),
            Map.entry(ItemComponentKeys.SHULKER_COLOR, methods("shulkerColor", "withShulkerColor", "withoutShulkerColor")));

    private static String[] methods(String getter, String setter, String remover) {
        return new String[] {getter, setter, remover};
    }
}
