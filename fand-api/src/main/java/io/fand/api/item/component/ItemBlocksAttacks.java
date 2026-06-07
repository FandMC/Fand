package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:blocks_attacks}. */
public record ItemBlocksAttacks(
        float blockDelaySeconds,
        float disableCooldownScale,
        List<ItemBlocksAttacks.DamageReduction> damageReductions,
        ItemDamageFunction itemDamage,
        Optional<ItemKeySet> bypassedBy,
        Optional<Key> blockSound,
        Optional<Key> disableSound) implements ItemComponentData {

    public static final ItemDamageFunction DEFAULT_ITEM_DAMAGE = new ItemDamageFunction(1.0F, 0.0F, 1.0F);
    public static final DamageReduction DEFAULT_DAMAGE_REDUCTION =
            new DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F);

    public ItemBlocksAttacks {
        if (blockDelaySeconds < 0.0F) {
            throw new IllegalArgumentException("blockDelaySeconds must be >= 0");
        }
        if (disableCooldownScale < 0.0F) {
            throw new IllegalArgumentException("disableCooldownScale must be >= 0");
        }
        damageReductions = List.copyOf(Objects.requireNonNull(damageReductions, "damageReductions"));
        itemDamage = Objects.requireNonNull(itemDamage, "itemDamage");
        bypassedBy = Objects.requireNonNull(bypassedBy, "bypassedBy");
        blockSound = Objects.requireNonNull(blockSound, "blockSound");
        disableSound = Objects.requireNonNull(disableSound, "disableSound");
    }

    public static ItemBlocksAttacks fromJson(JsonElement value) {
        var object = ItemComponentJson.objectOrEmpty(value);
        var reductions = new java.util.ArrayList<DamageReduction>();
        var rawReductions = object.get("damage_reductions");
        if (rawReductions != null && rawReductions.isJsonArray()) {
            for (var reduction : rawReductions.getAsJsonArray()) {
                reductions.add(DamageReduction.fromJson(reduction));
            }
        }
        if (reductions.isEmpty()) {
            reductions.add(DEFAULT_DAMAGE_REDUCTION);
        }
        return new ItemBlocksAttacks(
                ItemComponentJson.floatOr(object, "block_delay_seconds", 0.0F),
                ItemComponentJson.floatOr(object, "disable_cooldown_scale", 1.0F),
                reductions,
                object.has("item_damage") ? ItemDamageFunction.fromJson(object.get("item_damage")) : DEFAULT_ITEM_DAMAGE,
                object.has("bypassed_by") ? Optional.of(ItemKeySet.fromJson(object.get("bypassed_by"))) : Optional.empty(),
                ItemComponentJson.optionalKey(object, "block_sound"),
                ItemComponentJson.optionalKey(object, "disabled_sound"));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        if (blockDelaySeconds != 0.0F) {
            json.addProperty("block_delay_seconds", blockDelaySeconds);
        }
        if (disableCooldownScale != 1.0F) {
            json.addProperty("disable_cooldown_scale", disableCooldownScale);
        }
        if (!damageReductions.equals(List.of(DEFAULT_DAMAGE_REDUCTION))) {
            var reductions = new JsonArray();
            damageReductions.forEach(reduction -> reductions.add(reduction.toJson()));
            json.add("damage_reductions", reductions);
        }
        if (!itemDamage.equals(DEFAULT_ITEM_DAMAGE)) {
            json.add("item_damage", itemDamage.toJson());
        }
        bypassedBy.ifPresent(value -> json.add("bypassed_by", value.toJson()));
        blockSound.ifPresent(value -> json.addProperty("block_sound", value.asString()));
        disableSound.ifPresent(value -> json.addProperty("disabled_sound", value.asString()));
        return json;
    }

    public record DamageReduction(float horizontalBlockingAngle, Optional<ItemKeySet> type, float base, float factor) implements ItemComponentData {

        public DamageReduction {
            if (horizontalBlockingAngle <= 0.0F) {
                throw new IllegalArgumentException("horizontalBlockingAngle must be > 0");
            }
            type = Objects.requireNonNull(type, "type");
        }

        public static DamageReduction fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "damage reduction");
            return new DamageReduction(
                    ItemComponentJson.floatOr(object, "horizontal_blocking_angle", 90.0F),
                    object.has("type") ? Optional.of(ItemKeySet.fromJson(object.get("type"))) : Optional.empty(),
                    object.get("base").getAsFloat(),
                    object.get("factor").getAsFloat());
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            if (horizontalBlockingAngle != 90.0F) {
                json.addProperty("horizontal_blocking_angle", horizontalBlockingAngle);
            }
            type.ifPresent(value -> json.add("type", value.toJson()));
            json.addProperty("base", base);
            json.addProperty("factor", factor);
            return json;
        }
    }

    public record ItemDamageFunction(float threshold, float base, float factor) implements ItemComponentData {

        public ItemDamageFunction {
            if (threshold < 0.0F) {
                throw new IllegalArgumentException("threshold must be >= 0");
            }
        }

        public static ItemDamageFunction fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "item damage function");
            return new ItemDamageFunction(
                    object.get("threshold").getAsFloat(),
                    object.get("base").getAsFloat(),
                    object.get("factor").getAsFloat());
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("threshold", threshold);
            json.addProperty("base", base);
            json.addProperty("factor", factor);
            return json;
        }
    }
}
