package io.fand.api.enchantment;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

public sealed interface EnchantmentLocationEffect extends EnchantmentJsonValue
        permits EnchantmentEntityEffect,
        EnchantmentLocationEffect.AllOf,
        EnchantmentLocationEffect.Attribute,
        EnchantmentLocationEffect.Raw {

    static EnchantmentLocationEffect raw(JsonObject value) {
        return new Raw(value);
    }

    record AllOf(List<EnchantmentLocationEffect> effects) implements EnchantmentLocationEffect {
        public AllOf {
            effects = List.copyOf(effects);
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:all_of");
            json.add("effects", EnchantmentJson.array(effects));
            return json;
        }
    }

    record Attribute(Key id, Key attribute, EnchantmentLevelValue amount, EnchantmentAttributeOperation operation) implements EnchantmentLocationEffect {
        public Attribute {
            id = Objects.requireNonNull(id, "id");
            attribute = Objects.requireNonNull(attribute, "attribute");
            amount = Objects.requireNonNull(amount, "amount");
            operation = Objects.requireNonNull(operation, "operation");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:attribute");
            json.addProperty("id", id.asString());
            json.addProperty("attribute", attribute.asString());
            json.add("amount", amount.toJson());
            json.addProperty("operation", operation.serializedName());
            return json;
        }
    }

    record Raw(JsonObject value) implements EnchantmentLocationEffect {
        public Raw {
            value = EnchantmentJson.rawObject(value);
        }

        @Override
        public JsonObject toJson() {
            return value.deepCopy();
        }
    }
}
