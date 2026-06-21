package io.fand.api.enchantment;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;

public sealed interface EnchantmentValueEffect extends EnchantmentJsonValue
        permits EnchantmentValueEffect.Add,
        EnchantmentValueEffect.AllOf,
        EnchantmentValueEffect.Multiply,
        EnchantmentValueEffect.Raw,
        EnchantmentValueEffect.RemoveBinomial,
        EnchantmentValueEffect.ScaleExponentially,
        EnchantmentValueEffect.Set {

    static EnchantmentValueEffect add(EnchantmentLevelValue value) {
        return new Add(value);
    }

    static EnchantmentValueEffect multiply(EnchantmentLevelValue factor) {
        return new Multiply(factor);
    }

    static EnchantmentValueEffect set(EnchantmentLevelValue value) {
        return new Set(value);
    }

    static EnchantmentValueEffect raw(JsonObject value) {
        return new Raw(value);
    }

    record Add(EnchantmentLevelValue value) implements EnchantmentValueEffect {
        public Add {
            value = Objects.requireNonNull(value, "value");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:add");
            json.add("value", value.toJson());
            return json;
        }
    }

    record AllOf(List<EnchantmentValueEffect> effects) implements EnchantmentValueEffect {
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

    record Multiply(EnchantmentLevelValue factor) implements EnchantmentValueEffect {
        public Multiply {
            factor = Objects.requireNonNull(factor, "factor");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:multiply");
            json.add("factor", factor.toJson());
            return json;
        }
    }

    record RemoveBinomial(EnchantmentLevelValue chance) implements EnchantmentValueEffect {
        public RemoveBinomial {
            chance = Objects.requireNonNull(chance, "chance");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:remove_binomial");
            json.add("chance", chance.toJson());
            return json;
        }
    }

    record ScaleExponentially(EnchantmentLevelValue base, EnchantmentLevelValue exponent) implements EnchantmentValueEffect {
        public ScaleExponentially {
            base = Objects.requireNonNull(base, "base");
            exponent = Objects.requireNonNull(exponent, "exponent");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:exponential");
            json.add("base", base.toJson());
            json.add("exponent", exponent.toJson());
            return json;
        }
    }

    record Set(EnchantmentLevelValue value) implements EnchantmentValueEffect {
        public Set {
            value = Objects.requireNonNull(value, "value");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:set");
            json.add("value", value.toJson());
            return json;
        }
    }

    record Raw(JsonObject value) implements EnchantmentValueEffect {
        public Raw {
            value = EnchantmentJson.rawObject(value);
        }

        @Override
        public JsonObject toJson() {
            return value.deepCopy();
        }
    }
}
