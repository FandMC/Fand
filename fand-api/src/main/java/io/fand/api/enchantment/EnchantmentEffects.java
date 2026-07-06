package io.fand.api.enchantment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

public record EnchantmentEffects(JsonObject value) {

    public static final EnchantmentEffects EMPTY = new EnchantmentEffects(new JsonObject());

    public EnchantmentEffects {
        value = value == null ? new JsonObject() : value.deepCopy();
    }

    public static EnchantmentEffects emptyEffects() {
        return EMPTY;
    }

    public static EnchantmentEffects raw(JsonObject value) {
        return new EnchantmentEffects(value);
    }

    public static Builder builder() {
        return new Builder();
    }

    public JsonObject toJson() {
        return value.deepCopy();
    }

    public boolean empty() {
        return value.size() == 0;
    }

    public Builder toBuilder() {
        return new Builder(value);
    }

    public static final class Builder {
        private final JsonObject value;

        private Builder() {
            this.value = new JsonObject();
        }

        private Builder(JsonObject value) {
            this.value = value.deepCopy();
        }

        public Builder damageProtection(EnchantmentValueEffect effect) {
            return addConditional("minecraft:damage_protection", EnchantmentEffectEntry.of(effect));
        }

        public Builder damageProtection(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:damage_protection", effect);
        }

        public Builder damageImmunity() {
            return addConditional("minecraft:damage_immunity", EnchantmentEffectEntry.of(new UnitEffect()));
        }

        public Builder damageImmunity(EnchantmentCondition requirements) {
            return addConditional("minecraft:damage_immunity", EnchantmentEffectEntry.of(new UnitEffect(), requirements));
        }

        public Builder damage(EnchantmentValueEffect effect) {
            return addConditional("minecraft:damage", EnchantmentEffectEntry.of(effect));
        }

        public Builder damage(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:damage", effect);
        }

        public Builder smashDamagePerFallenBlock(EnchantmentValueEffect effect) {
            return addConditional("minecraft:smash_damage_per_fallen_block", EnchantmentEffectEntry.of(effect));
        }

        public Builder smashDamagePerFallenBlock(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:smash_damage_per_fallen_block", effect);
        }

        public Builder knockback(EnchantmentValueEffect effect) {
            return addConditional("minecraft:knockback", EnchantmentEffectEntry.of(effect));
        }

        public Builder knockback(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:knockback", effect);
        }

        public Builder armorEffectiveness(EnchantmentValueEffect effect) {
            return addConditional("minecraft:armor_effectiveness", EnchantmentEffectEntry.of(effect));
        }

        public Builder armorEffectiveness(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:armor_effectiveness", effect);
        }

        public Builder postAttack(EnchantmentTarget enchanted, EnchantmentTarget affected, EnchantmentEntityEffect effect) {
            return addTargeted("minecraft:post_attack", EnchantmentTargetedEffectEntry.of(enchanted, affected, effect));
        }

        public Builder postAttack(EnchantmentTargetedEffectEntry<EnchantmentEntityEffect> effect) {
            return addTargeted("minecraft:post_attack", effect);
        }

        public Builder postPiercingAttack(EnchantmentEntityEffect effect) {
            return addConditional("minecraft:post_piercing_attack", EnchantmentEffectEntry.of(effect));
        }

        public Builder postPiercingAttack(EnchantmentEffectEntry<EnchantmentEntityEffect> effect) {
            return addConditional("minecraft:post_piercing_attack", effect);
        }

        public Builder hitBlock(EnchantmentEntityEffect effect) {
            return addConditional("minecraft:hit_block", EnchantmentEffectEntry.of(effect));
        }

        public Builder hitBlock(EnchantmentEffectEntry<EnchantmentEntityEffect> effect) {
            return addConditional("minecraft:hit_block", effect);
        }

        public Builder itemDamage(EnchantmentValueEffect effect) {
            return addConditional("minecraft:item_damage", EnchantmentEffectEntry.of(effect));
        }

        public Builder itemDamage(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:item_damage", effect);
        }

        public Builder equipmentDrops(EnchantmentTarget enchanted, EnchantmentValueEffect effect) {
            var entry = new JsonObject();
            entry.addProperty("enchanted", Objects.requireNonNull(enchanted, "enchanted").serializedName());
            entry.add("effect", Objects.requireNonNull(effect, "effect").toJson());
            return add("minecraft:equipment_drops", entry);
        }

        public Builder equipmentDrops(EnchantmentTarget enchanted, EnchantmentValueEffect effect, EnchantmentCondition requirements) {
            var entry = new JsonObject();
            entry.addProperty("enchanted", Objects.requireNonNull(enchanted, "enchanted").serializedName());
            entry.add("effect", Objects.requireNonNull(effect, "effect").toJson());
            entry.add("requirements", Objects.requireNonNull(requirements, "requirements").toJson());
            return add("minecraft:equipment_drops", entry);
        }

        public Builder locationChanged(EnchantmentLocationEffect effect) {
            return addConditional("minecraft:location_changed", EnchantmentEffectEntry.of(effect));
        }

        public Builder locationChanged(EnchantmentEffectEntry<EnchantmentLocationEffect> effect) {
            return addConditional("minecraft:location_changed", effect);
        }

        public Builder tick(EnchantmentEntityEffect effect) {
            return addConditional("minecraft:tick", EnchantmentEffectEntry.of(effect));
        }

        public Builder tick(EnchantmentEffectEntry<EnchantmentEntityEffect> effect) {
            return addConditional("minecraft:tick", effect);
        }

        public Builder ammoUse(EnchantmentValueEffect effect) {
            return addConditional("minecraft:ammo_use", EnchantmentEffectEntry.of(effect));
        }

        public Builder ammoUse(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:ammo_use", effect);
        }

        public Builder projectilePiercing(EnchantmentValueEffect effect) {
            return addConditional("minecraft:projectile_piercing", EnchantmentEffectEntry.of(effect));
        }

        public Builder projectilePiercing(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:projectile_piercing", effect);
        }

        public Builder projectileSpawned(EnchantmentEntityEffect effect) {
            return addConditional("minecraft:projectile_spawned", EnchantmentEffectEntry.of(effect));
        }

        public Builder projectileSpawned(EnchantmentEffectEntry<EnchantmentEntityEffect> effect) {
            return addConditional("minecraft:projectile_spawned", effect);
        }

        public Builder projectileSpread(EnchantmentValueEffect effect) {
            return addConditional("minecraft:projectile_spread", EnchantmentEffectEntry.of(effect));
        }

        public Builder projectileSpread(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:projectile_spread", effect);
        }

        public Builder projectileCount(EnchantmentValueEffect effect) {
            return addConditional("minecraft:projectile_count", EnchantmentEffectEntry.of(effect));
        }

        public Builder projectileCount(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:projectile_count", effect);
        }

        public Builder tridentReturnAcceleration(EnchantmentValueEffect effect) {
            return addConditional("minecraft:trident_return_acceleration", EnchantmentEffectEntry.of(effect));
        }

        public Builder tridentReturnAcceleration(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:trident_return_acceleration", effect);
        }

        public Builder fishingTimeReduction(EnchantmentValueEffect effect) {
            return addConditional("minecraft:fishing_time_reduction", EnchantmentEffectEntry.of(effect));
        }

        public Builder fishingTimeReduction(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:fishing_time_reduction", effect);
        }

        public Builder fishingLuckBonus(EnchantmentValueEffect effect) {
            return addConditional("minecraft:fishing_luck_bonus", EnchantmentEffectEntry.of(effect));
        }

        public Builder fishingLuckBonus(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:fishing_luck_bonus", effect);
        }

        public Builder blockExperience(EnchantmentValueEffect effect) {
            return addConditional("minecraft:block_experience", EnchantmentEffectEntry.of(effect));
        }

        public Builder blockExperience(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:block_experience", effect);
        }

        public Builder mobExperience(EnchantmentValueEffect effect) {
            return addConditional("minecraft:mob_experience", EnchantmentEffectEntry.of(effect));
        }

        public Builder mobExperience(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:mob_experience", effect);
        }

        public Builder repairWithXp(EnchantmentValueEffect effect) {
            return addConditional("minecraft:repair_with_xp", EnchantmentEffectEntry.of(effect));
        }

        public Builder repairWithXp(EnchantmentEffectEntry<EnchantmentValueEffect> effect) {
            return addConditional("minecraft:repair_with_xp", effect);
        }

        public Builder attribute(Key id, Key attribute, EnchantmentLevelValue amount, EnchantmentAttributeOperation operation) {
            var json = new JsonObject();
            json.addProperty("id", Objects.requireNonNull(id, "id").asString());
            json.addProperty("attribute", Objects.requireNonNull(attribute, "attribute").asString());
            json.add("amount", Objects.requireNonNull(amount, "amount").toJson());
            json.addProperty("operation", Objects.requireNonNull(operation, "operation").serializedName());
            return add("minecraft:attributes", json);
        }

        public Builder crossbowChargeTime(EnchantmentValueEffect effect) {
            return set("minecraft:crossbow_charge_time", effect.toJson());
        }

        public Builder crossbowChargingSound(Key start, Key mid, Key end) {
            var json = new JsonObject();
            json.addProperty("start", Objects.requireNonNull(start, "start").asString());
            json.addProperty("mid", Objects.requireNonNull(mid, "mid").asString());
            json.addProperty("end", Objects.requireNonNull(end, "end").asString());
            return add("minecraft:crossbow_charging_sounds", json);
        }

        public Builder tridentSound(Key sound) {
            return add("minecraft:trident_sound", new com.google.gson.JsonPrimitive(Objects.requireNonNull(sound, "sound").asString()));
        }

        public Builder preventEquipmentDrop() {
            return set("minecraft:prevent_equipment_drop", new JsonObject());
        }

        public Builder preventArmorChange() {
            return set("minecraft:prevent_armor_change", new JsonObject());
        }

        public Builder tridentSpinAttackStrength(EnchantmentValueEffect effect) {
            return set("minecraft:trident_spin_attack_strength", effect.toJson());
        }

        public Builder component(Key component, JsonElement rawValue) {
            return set(component.asString(), rawValue);
        }

        public Builder addComponentEntry(Key component, JsonElement rawEntry) {
            return add(component.asString(), rawEntry);
        }

        public EnchantmentEffects build() {
            return new EnchantmentEffects(value);
        }

        private Builder addConditional(String component, EnchantmentEffectEntry<? extends EnchantmentJsonValue> effect) {
            return add(component, effect.toJson());
        }

        private Builder addTargeted(String component, EnchantmentTargetedEffectEntry<? extends EnchantmentJsonValue> effect) {
            return add(component, effect.toJson());
        }

        private Builder add(String component, JsonElement entry) {
            var array = value.getAsJsonArray(component);
            if (array == null) {
                array = new JsonArray();
                value.add(component, array);
            }
            array.add(EnchantmentJson.copy(entry));
            return this;
        }

        private Builder set(String component, JsonElement rawValue) {
            value.add(component, EnchantmentJson.copy(rawValue));
            return this;
        }
    }

    private record UnitEffect() implements EnchantmentJsonValue {
        @Override
        public JsonObject toJson() {
            return new JsonObject();
        }
    }
}
