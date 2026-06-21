package io.fand.api.enchantment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record EnchantmentCondition(JsonObject value) implements EnchantmentJsonValue {

    public EnchantmentCondition {
        value = EnchantmentJson.rawObject(value);
    }

    public static EnchantmentCondition raw(JsonObject value) {
        return new EnchantmentCondition(value);
    }

    @Override
    public JsonElement toJson() {
        return value.deepCopy();
    }
}
