package io.fand.api.enchantment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record EnchantmentTargetedEffectEntry<T extends EnchantmentJsonValue>(
        EnchantmentTarget enchanted,
        EnchantmentTarget affected,
        T effect,
        @Nullable EnchantmentCondition requirements
) implements EnchantmentJsonValue {

    public EnchantmentTargetedEffectEntry {
        enchanted = Objects.requireNonNull(enchanted, "enchanted");
        affected = Objects.requireNonNull(affected, "affected");
        effect = Objects.requireNonNull(effect, "effect");
    }

    public static <T extends EnchantmentJsonValue> EnchantmentTargetedEffectEntry<T> of(
            EnchantmentTarget enchanted,
            EnchantmentTarget affected,
            T effect) {
        return new EnchantmentTargetedEffectEntry<>(enchanted, affected, effect, null);
    }

    public static <T extends EnchantmentJsonValue> EnchantmentTargetedEffectEntry<T> of(
            EnchantmentTarget enchanted,
            EnchantmentTarget affected,
            T effect,
            EnchantmentCondition requirements) {
        return new EnchantmentTargetedEffectEntry<>(enchanted, affected, effect, requirements);
    }

    public Optional<EnchantmentCondition> optionalRequirements() {
        return Optional.ofNullable(requirements);
    }

    @Override
    public JsonElement toJson() {
        var json = new JsonObject();
        json.addProperty("enchanted", enchanted.serializedName());
        json.addProperty("affected", affected.serializedName());
        json.add("effect", effect.toJson());
        if (requirements != null) {
            json.add("requirements", requirements.toJson());
        }
        return json;
    }
}
