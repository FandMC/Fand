package io.fand.api.advancement;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

public final class AdvancementTriggers {

    public static final Key IMPOSSIBLE = minecraft("impossible");
    public static final Key PLAYER_KILLED_ENTITY = minecraft("player_killed_entity");
    public static final Key ENTITY_KILLED_PLAYER = minecraft("entity_killed_player");
    public static final Key ENTER_BLOCK = minecraft("enter_block");
    public static final Key INVENTORY_CHANGED = minecraft("inventory_changed");
    public static final Key RECIPE_UNLOCKED = minecraft("recipe_unlocked");
    public static final Key PLAYER_HURT_ENTITY = minecraft("player_hurt_entity");
    public static final Key ENTITY_HURT_PLAYER = minecraft("entity_hurt_player");
    public static final Key ENCHANTED_ITEM = minecraft("enchanted_item");
    public static final Key FILLED_BUCKET = minecraft("filled_bucket");
    public static final Key BREWED_POTION = minecraft("brewed_potion");
    public static final Key CONSTRUCT_BEACON = minecraft("construct_beacon");
    public static final Key USED_ENDER_EYE = minecraft("used_ender_eye");
    public static final Key SUMMONED_ENTITY = minecraft("summoned_entity");
    public static final Key BRED_ANIMALS = minecraft("bred_animals");
    public static final Key LOCATION = minecraft("location");
    public static final Key SLEPT_IN_BED = minecraft("slept_in_bed");
    public static final Key CURED_ZOMBIE_VILLAGER = minecraft("cured_zombie_villager");
    public static final Key VILLAGER_TRADE = minecraft("villager_trade");
    public static final Key ITEM_DURABILITY_CHANGED = minecraft("item_durability_changed");
    public static final Key LEVITATION = minecraft("levitation");
    public static final Key CHANGED_DIMENSION = minecraft("changed_dimension");
    public static final Key TICK = minecraft("tick");
    public static final Key TAME_ANIMAL = minecraft("tame_animal");
    public static final Key PLACED_BLOCK = minecraft("placed_block");
    public static final Key CONSUME_ITEM = minecraft("consume_item");
    public static final Key EFFECTS_CHANGED = minecraft("effects_changed");
    public static final Key USED_TOTEM = minecraft("used_totem");
    public static final Key NETHER_TRAVEL = minecraft("nether_travel");
    public static final Key FISHING_ROD_HOOKED = minecraft("fishing_rod_hooked");
    public static final Key CHANNELED_LIGHTNING = minecraft("channeled_lightning");
    public static final Key SHOT_CROSSBOW = minecraft("shot_crossbow");
    public static final Key SPEAR_MOBS = minecraft("spear_mobs");
    public static final Key KILLED_BY_ARROW = minecraft("killed_by_arrow");
    public static final Key HERO_OF_THE_VILLAGE = minecraft("hero_of_the_village");
    public static final Key VOLUNTARY_EXILE = minecraft("voluntary_exile");
    public static final Key SLIDE_DOWN_BLOCK = minecraft("slide_down_block");
    public static final Key BEE_NEST_DESTROYED = minecraft("bee_nest_destroyed");
    public static final Key TARGET_HIT = minecraft("target_hit");
    public static final Key ITEM_USED_ON_BLOCK = minecraft("item_used_on_block");
    public static final Key DEFAULT_BLOCK_USE = minecraft("default_block_use");
    public static final Key ANY_BLOCK_USE = minecraft("any_block_use");
    public static final Key PLAYER_GENERATES_CONTAINER_LOOT = minecraft("player_generates_container_loot");
    public static final Key THROWN_ITEM_PICKED_UP_BY_ENTITY = minecraft("thrown_item_picked_up_by_entity");
    public static final Key THROWN_ITEM_PICKED_UP_BY_PLAYER = minecraft("thrown_item_picked_up_by_player");
    public static final Key PLAYER_INTERACTED_WITH_ENTITY = minecraft("player_interacted_with_entity");
    public static final Key PLAYER_SHEARED_EQUIPMENT = minecraft("player_sheared_equipment");
    public static final Key STARTED_RIDING = minecraft("started_riding");
    public static final Key LIGHTNING_STRIKE = minecraft("lightning_strike");
    public static final Key USING_ITEM = minecraft("using_item");
    public static final Key FALL_FROM_HEIGHT = minecraft("fall_from_height");
    public static final Key RIDE_ENTITY_IN_LAVA = minecraft("ride_entity_in_lava");
    public static final Key KILL_MOB_NEAR_SCULK_CATALYST = minecraft("kill_mob_near_sculk_catalyst");
    public static final Key ALLAY_DROP_ITEM_ON_BLOCK = minecraft("allay_drop_item_on_block");
    public static final Key AVOID_VIBRATION = minecraft("avoid_vibration");
    public static final Key RECIPE_CRAFTED = minecraft("recipe_crafted");
    public static final Key CRAFTER_RECIPE_CRAFTED = minecraft("crafter_recipe_crafted");
    public static final Key FALL_AFTER_EXPLOSION = minecraft("fall_after_explosion");

    private AdvancementTriggers() {
    }

    public static AdvancementTrigger raw(Key trigger) {
        return AdvancementTrigger.builder(trigger).build();
    }

    public static AdvancementTrigger raw(Key trigger, JsonObject conditions) {
        return AdvancementTrigger.builder(trigger).conditions(conditions).build();
    }

    public static AdvancementTrigger impossible() {
        return raw(IMPOSSIBLE);
    }

    public static AdvancementTrigger inventoryChanged(AdvancementItemPredicate... items) {
        var conditions = new JsonObject();
        conditions.add("items", itemArray(items));
        return raw(INVENTORY_CHANGED, conditions);
    }

    public static AdvancementTrigger inventoryChanged(List<AdvancementItemPredicate> items) {
        var conditions = new JsonObject();
        conditions.add("items", itemArray(items));
        return raw(INVENTORY_CHANGED, conditions);
    }

    public static AdvancementTrigger inventoryChangedSlots(AdvancementRange occupied, AdvancementRange full, AdvancementRange empty) {
        var slots = new JsonObject();
        if (!Objects.requireNonNull(occupied, "occupied").any()) {
            slots.add("occupied", occupied.toJson());
        }
        if (!Objects.requireNonNull(full, "full").any()) {
            slots.add("full", full.toJson());
        }
        if (!Objects.requireNonNull(empty, "empty").any()) {
            slots.add("empty", empty.toJson());
        }
        var conditions = new JsonObject();
        conditions.add("slots", slots);
        return raw(INVENTORY_CHANGED, conditions);
    }

    public static AdvancementTrigger consumeItem(AdvancementItemPredicate item) {
        return AdvancementTrigger.builder(CONSUME_ITEM).item(item).build();
    }

    public static AdvancementTrigger usingItem(AdvancementItemPredicate item) {
        return AdvancementTrigger.builder(USING_ITEM).item(item).build();
    }

    public static AdvancementTrigger usedTotem(AdvancementItemPredicate item) {
        return AdvancementTrigger.builder(USED_TOTEM).item(item).build();
    }

    public static AdvancementTrigger filledBucket(AdvancementItemPredicate item) {
        return AdvancementTrigger.builder(FILLED_BUCKET).item(item).build();
    }

    public static AdvancementTrigger enchantedItem(AdvancementItemPredicate item, AdvancementRange levels) {
        var conditions = new JsonObject();
        conditions.add("item", Objects.requireNonNull(item, "item").toJson());
        conditions.add("levels", Objects.requireNonNull(levels, "levels").toJson());
        return raw(ENCHANTED_ITEM, conditions);
    }

    public static AdvancementTrigger playerKilledEntity(AdvancementEntityPredicate entity) {
        return entityTrigger(PLAYER_KILLED_ENTITY, entity);
    }

    public static AdvancementTrigger entityKilledPlayer(AdvancementEntityPredicate entity) {
        return entityTrigger(ENTITY_KILLED_PLAYER, entity);
    }

    public static AdvancementTrigger playerHurtEntity(AdvancementDamagePredicate damage, AdvancementEntityPredicate entity) {
        return AdvancementTrigger.builder(PLAYER_HURT_ENTITY).damage(damage).entity(entity).build();
    }

    public static AdvancementTrigger entityHurtPlayer(AdvancementDamagePredicate damage) {
        return AdvancementTrigger.builder(ENTITY_HURT_PLAYER).damage(damage).build();
    }

    public static AdvancementTrigger location(AdvancementLocationPredicate location) {
        return AdvancementTrigger.builder(LOCATION).player(AdvancementEntityPredicate.builder().located(location).build()).build();
    }

    public static AdvancementTrigger tick() {
        return raw(TICK);
    }

    public static AdvancementTrigger sleptInBed() {
        return raw(SLEPT_IN_BED);
    }

    public static AdvancementTrigger changedDimension(Key from, Key to) {
        var conditions = new JsonObject();
        if (from != null) {
            conditions.addProperty("from", from.asString());
        }
        if (to != null) {
            conditions.addProperty("to", to.asString());
        }
        return raw(CHANGED_DIMENSION, conditions);
    }

    public static AdvancementTrigger recipeUnlocked(Key recipe) {
        var conditions = new JsonObject();
        conditions.addProperty("recipe", Objects.requireNonNull(recipe, "recipe").asString());
        return raw(RECIPE_UNLOCKED, conditions);
    }

    public static AdvancementTrigger recipeCrafted(Key recipe, AdvancementItemPredicate... ingredients) {
        var conditions = new JsonObject();
        conditions.addProperty("recipe_id", Objects.requireNonNull(recipe, "recipe").asString());
        conditions.add("ingredients", itemArray(ingredients));
        return raw(RECIPE_CRAFTED, conditions);
    }

    public static AdvancementTrigger generatedLoot(Key lootTable) {
        var conditions = new JsonObject();
        conditions.addProperty("loot_table", Objects.requireNonNull(lootTable, "lootTable").asString());
        return raw(PLAYER_GENERATES_CONTAINER_LOOT, conditions);
    }

    public static AdvancementTrigger distance(Key trigger, AdvancementDistancePredicate distance) {
        var conditions = new JsonObject();
        conditions.add("distance", Objects.requireNonNull(distance, "distance").toJson());
        return raw(trigger, conditions);
    }

    public static AdvancementTrigger itemUsedOnBlock(AdvancementLocationPredicate location) {
        return AdvancementTrigger.builder(ITEM_USED_ON_BLOCK).location(location).build();
    }

    public static AdvancementTrigger placedBlock(AdvancementLocationPredicate location) {
        return AdvancementTrigger.builder(PLACED_BLOCK).location(location).build();
    }

    public static AdvancementTrigger playerInteractedWithEntity(AdvancementItemPredicate item, AdvancementEntityPredicate entity) {
        return AdvancementTrigger.builder(PLAYER_INTERACTED_WITH_ENTITY).item(item).entity(entity).build();
    }

    private static AdvancementTrigger entityTrigger(Key trigger, AdvancementEntityPredicate entity) {
        return AdvancementTrigger.builder(trigger).entity(entity).build();
    }

    private static JsonArray itemArray(AdvancementItemPredicate... items) {
        return itemArray(Arrays.asList(items));
    }

    private static JsonArray itemArray(List<AdvancementItemPredicate> items) {
        var array = new JsonArray();
        items.stream()
                .map(item -> Objects.requireNonNull(item, "item").toJson())
                .forEach(array::add);
        return array;
    }

    private static Key minecraft(String value) {
        return Key.key(Key.MINECRAFT_NAMESPACE, value);
    }
}
