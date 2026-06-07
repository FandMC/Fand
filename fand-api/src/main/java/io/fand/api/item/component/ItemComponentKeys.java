package io.fand.api.item.component;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.kyori.adventure.key.Key;

/**
 * Vanilla item data component keys.
 */
public final class ItemComponentKeys {

    public static final Key CUSTOM_DATA = Key.key("minecraft:custom_data");
    public static final Key MAX_STACK_SIZE = Key.key("minecraft:max_stack_size");
    public static final Key MAX_DAMAGE = Key.key("minecraft:max_damage");
    public static final Key DAMAGE = Key.key("minecraft:damage");
    public static final Key UNBREAKABLE = Key.key("minecraft:unbreakable");
    public static final Key USE_EFFECTS = Key.key("minecraft:use_effects");
    public static final Key CUSTOM_NAME = Key.key("minecraft:custom_name");
    public static final Key MINIMUM_ATTACK_CHARGE = Key.key("minecraft:minimum_attack_charge");
    public static final Key DAMAGE_TYPE = Key.key("minecraft:damage_type");
    public static final Key ITEM_NAME = Key.key("minecraft:item_name");
    public static final Key ITEM_MODEL = Key.key("minecraft:item_model");
    public static final Key LORE = Key.key("minecraft:lore");
    public static final Key RARITY = Key.key("minecraft:rarity");
    public static final Key ENCHANTMENTS = Key.key("minecraft:enchantments");
    public static final Key CAN_PLACE_ON = Key.key("minecraft:can_place_on");
    public static final Key CAN_BREAK = Key.key("minecraft:can_break");
    public static final Key ATTRIBUTE_MODIFIERS = Key.key("minecraft:attribute_modifiers");
    public static final Key CUSTOM_MODEL_DATA = Key.key("minecraft:custom_model_data");
    public static final Key TOOLTIP_DISPLAY = Key.key("minecraft:tooltip_display");
    public static final Key REPAIR_COST = Key.key("minecraft:repair_cost");
    public static final Key CREATIVE_SLOT_LOCK = Key.key("minecraft:creative_slot_lock");
    public static final Key ENCHANTMENT_GLINT_OVERRIDE = Key.key("minecraft:enchantment_glint_override");
    public static final Key INTANGIBLE_PROJECTILE = Key.key("minecraft:intangible_projectile");
    public static final Key FOOD = Key.key("minecraft:food");
    public static final Key CONSUMABLE = Key.key("minecraft:consumable");
    public static final Key USE_REMAINDER = Key.key("minecraft:use_remainder");
    public static final Key USE_COOLDOWN = Key.key("minecraft:use_cooldown");
    public static final Key DAMAGE_RESISTANT = Key.key("minecraft:damage_resistant");
    public static final Key TOOL = Key.key("minecraft:tool");
    public static final Key WEAPON = Key.key("minecraft:weapon");
    public static final Key ATTACK_RANGE = Key.key("minecraft:attack_range");
    public static final Key ENCHANTABLE = Key.key("minecraft:enchantable");
    public static final Key EQUIPPABLE = Key.key("minecraft:equippable");
    public static final Key REPAIRABLE = Key.key("minecraft:repairable");
    public static final Key GLIDER = Key.key("minecraft:glider");
    public static final Key TOOLTIP_STYLE = Key.key("minecraft:tooltip_style");
    public static final Key DEATH_PROTECTION = Key.key("minecraft:death_protection");
    public static final Key BLOCKS_ATTACKS = Key.key("minecraft:blocks_attacks");
    public static final Key PIERCING_WEAPON = Key.key("minecraft:piercing_weapon");
    public static final Key KINETIC_WEAPON = Key.key("minecraft:kinetic_weapon");
    public static final Key SWING_ANIMATION = Key.key("minecraft:swing_animation");
    public static final Key ADDITIONAL_TRADE_COST = Key.key("minecraft:additional_trade_cost");
    public static final Key STORED_ENCHANTMENTS = Key.key("minecraft:stored_enchantments");
    public static final Key DYE = Key.key("minecraft:dye");
    public static final Key DYED_COLOR = Key.key("minecraft:dyed_color");
    public static final Key MAP_COLOR = Key.key("minecraft:map_color");
    public static final Key MAP_ID = Key.key("minecraft:map_id");
    public static final Key MAP_DECORATIONS = Key.key("minecraft:map_decorations");
    public static final Key MAP_POST_PROCESSING = Key.key("minecraft:map_post_processing");
    public static final Key CHARGED_PROJECTILES = Key.key("minecraft:charged_projectiles");
    public static final Key BUNDLE_CONTENTS = Key.key("minecraft:bundle_contents");
    public static final Key POTION_CONTENTS = Key.key("minecraft:potion_contents");
    public static final Key POTION_DURATION_SCALE = Key.key("minecraft:potion_duration_scale");
    public static final Key SUSPICIOUS_STEW_EFFECTS = Key.key("minecraft:suspicious_stew_effects");
    public static final Key WRITABLE_BOOK_CONTENT = Key.key("minecraft:writable_book_content");
    public static final Key WRITTEN_BOOK_CONTENT = Key.key("minecraft:written_book_content");
    public static final Key TRIM = Key.key("minecraft:trim");
    public static final Key DEBUG_STICK_STATE = Key.key("minecraft:debug_stick_state");
    public static final Key ENTITY_DATA = Key.key("minecraft:entity_data");
    public static final Key BUCKET_ENTITY_DATA = Key.key("minecraft:bucket_entity_data");
    public static final Key BLOCK_ENTITY_DATA = Key.key("minecraft:block_entity_data");
    public static final Key INSTRUMENT = Key.key("minecraft:instrument");
    public static final Key PROVIDES_TRIM_MATERIAL = Key.key("minecraft:provides_trim_material");
    public static final Key OMINOUS_BOTTLE_AMPLIFIER = Key.key("minecraft:ominous_bottle_amplifier");
    public static final Key JUKEBOX_PLAYABLE = Key.key("minecraft:jukebox_playable");
    public static final Key PROVIDES_BANNER_PATTERNS = Key.key("minecraft:provides_banner_patterns");
    public static final Key RECIPES = Key.key("minecraft:recipes");
    public static final Key LODESTONE_TRACKER = Key.key("minecraft:lodestone_tracker");
    public static final Key FIREWORK_EXPLOSION = Key.key("minecraft:firework_explosion");
    public static final Key FIREWORKS = Key.key("minecraft:fireworks");
    public static final Key PROFILE = Key.key("minecraft:profile");
    public static final Key NOTE_BLOCK_SOUND = Key.key("minecraft:note_block_sound");
    public static final Key BANNER_PATTERNS = Key.key("minecraft:banner_patterns");
    public static final Key BASE_COLOR = Key.key("minecraft:base_color");
    public static final Key POT_DECORATIONS = Key.key("minecraft:pot_decorations");
    public static final Key CONTAINER = Key.key("minecraft:container");
    public static final Key BLOCK_STATE = Key.key("minecraft:block_state");
    public static final Key BEES = Key.key("minecraft:bees");
    public static final Key LOCK = Key.key("minecraft:lock");
    public static final Key CONTAINER_LOOT = Key.key("minecraft:container_loot");
    public static final Key BREAK_SOUND = Key.key("minecraft:break_sound");
    public static final Key VILLAGER_VARIANT = Key.key("minecraft:villager/variant");
    public static final Key WOLF_VARIANT = Key.key("minecraft:wolf/variant");
    public static final Key WOLF_SOUND_VARIANT = Key.key("minecraft:wolf/sound_variant");
    public static final Key WOLF_COLLAR = Key.key("minecraft:wolf/collar");
    public static final Key FOX_VARIANT = Key.key("minecraft:fox/variant");
    public static final Key SALMON_SIZE = Key.key("minecraft:salmon/size");
    public static final Key PARROT_VARIANT = Key.key("minecraft:parrot/variant");
    public static final Key TROPICAL_FISH_PATTERN = Key.key("minecraft:tropical_fish/pattern");
    public static final Key TROPICAL_FISH_BASE_COLOR = Key.key("minecraft:tropical_fish/base_color");
    public static final Key TROPICAL_FISH_PATTERN_COLOR = Key.key("minecraft:tropical_fish/pattern_color");
    public static final Key MOOSHROOM_VARIANT = Key.key("minecraft:mooshroom/variant");
    public static final Key RABBIT_VARIANT = Key.key("minecraft:rabbit/variant");
    public static final Key PIG_VARIANT = Key.key("minecraft:pig/variant");
    public static final Key PIG_SOUND_VARIANT = Key.key("minecraft:pig/sound_variant");
    public static final Key COW_VARIANT = Key.key("minecraft:cow/variant");
    public static final Key COW_SOUND_VARIANT = Key.key("minecraft:cow/sound_variant");
    public static final Key CHICKEN_VARIANT = Key.key("minecraft:chicken/variant");
    public static final Key CHICKEN_SOUND_VARIANT = Key.key("minecraft:chicken/sound_variant");
    public static final Key ZOMBIE_NAUTILUS_VARIANT = Key.key("minecraft:zombie_nautilus/variant");
    public static final Key FROG_VARIANT = Key.key("minecraft:frog/variant");
    public static final Key HORSE_VARIANT = Key.key("minecraft:horse/variant");
    public static final Key PAINTING_VARIANT = Key.key("minecraft:painting/variant");
    public static final Key LLAMA_VARIANT = Key.key("minecraft:llama/variant");
    public static final Key AXOLOTL_VARIANT = Key.key("minecraft:axolotl/variant");
    public static final Key CAT_VARIANT = Key.key("minecraft:cat/variant");
    public static final Key CAT_SOUND_VARIANT = Key.key("minecraft:cat/sound_variant");
    public static final Key CAT_COLLAR = Key.key("minecraft:cat/collar");
    public static final Key SHEEP_COLOR = Key.key("minecraft:sheep/color");
    public static final Key SHULKER_COLOR = Key.key("minecraft:shulker/color");

    private static final Set<Key> ALL = collectAll();

    private ItemComponentKeys() {
    }

    public static Set<Key> all() {
        return ALL;
    }

    public static boolean isKnown(Key key) {
        return ALL.contains(key);
    }

    private static Set<Key> collectAll() {
        var keys = new LinkedHashSet<Key>();
        for (var field : ItemComponentKeys.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (field.getType() == Key.class && Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
                try {
                    keys.add((Key) field.get(null));
                } catch (IllegalAccessException ex) {
                    throw new ExceptionInInitializerError(ex);
                }
            }
        }
        return Collections.unmodifiableSet(keys);
    }
}
