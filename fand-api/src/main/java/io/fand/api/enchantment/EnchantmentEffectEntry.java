package io.fand.api.enchantment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record EnchantmentEffectEntry<T extends EnchantmentJsonValue>(T effect, @Nullable EnchantmentCondition requirements)
        implements EnchantmentJsonValue {

    public EnchantmentEffectEntry {
        effect = Objects.requireNonNull(effect, "effect");
    }

    public static <T extends EnchantmentJsonValue> EnchantmentEffectEntry<T> of(T effect) {
        return new EnchantmentEffectEntry<>(effect, null);
    }

    public static <T extends EnchantmentJsonValue> EnchantmentEffectEntry<T> of(T effect, EnchantmentCondition requirements) {
        return new EnchantmentEffectEntry<>(effect, requirements);
    }

    public Optional<EnchantmentCondition> optionalRequirements() {
        return Optional.ofNullable(requirements);
    }

    @Override
    public JsonElement toJson() {
        var json = new JsonObject();
        json.add("effect", effect.toJson());
        if (requirements != null) {
            json.add("requirements", requirements.toJson());
        }
        return json;
    }
}
