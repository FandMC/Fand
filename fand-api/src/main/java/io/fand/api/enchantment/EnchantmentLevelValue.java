package io.fand.api.enchantment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;

public sealed interface EnchantmentLevelValue extends EnchantmentJsonValue
        permits EnchantmentLevelValue.Clamped,
        EnchantmentLevelValue.Constant,
        EnchantmentLevelValue.Exponent,
        EnchantmentLevelValue.Fraction,
        EnchantmentLevelValue.LevelsSquared,
        EnchantmentLevelValue.Linear,
        EnchantmentLevelValue.Lookup,
        EnchantmentLevelValue.Raw {

    static EnchantmentLevelValue constant(float value) {
        return new Constant(value);
    }

    static EnchantmentLevelValue linear(float base, float perLevelAboveFirst) {
        return new Linear(base, perLevelAboveFirst);
    }

    static EnchantmentLevelValue perLevel(float perLevel) {
        return linear(perLevel, perLevel);
    }

    static EnchantmentLevelValue levelsSquared(float added) {
        return new LevelsSquared(added);
    }

    static EnchantmentLevelValue raw(JsonObject value) {
        return new Raw(value);
    }

    record Constant(float value) implements EnchantmentLevelValue {
        @Override
        public JsonElement toJson() {
            return new com.google.gson.JsonPrimitive(value);
        }
    }

    record Clamped(EnchantmentLevelValue value, float min, float max) implements EnchantmentLevelValue {
        public Clamped {
            value = Objects.requireNonNull(value, "value");
            if (max <= min) {
                throw new IllegalArgumentException("max must be larger than min");
            }
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:clamped");
            json.add("value", value.toJson());
            json.addProperty("min", min);
            json.addProperty("max", max);
            return json;
        }
    }

    record Exponent(EnchantmentLevelValue base, EnchantmentLevelValue power) implements EnchantmentLevelValue {
        public Exponent {
            base = Objects.requireNonNull(base, "base");
            power = Objects.requireNonNull(power, "power");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:exponent");
            json.add("base", base.toJson());
            json.add("power", power.toJson());
            return json;
        }
    }

    record Fraction(EnchantmentLevelValue numerator, EnchantmentLevelValue denominator) implements EnchantmentLevelValue {
        public Fraction {
            numerator = Objects.requireNonNull(numerator, "numerator");
            denominator = Objects.requireNonNull(denominator, "denominator");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:fraction");
            json.add("numerator", numerator.toJson());
            json.add("denominator", denominator.toJson());
            return json;
        }
    }

    record LevelsSquared(float added) implements EnchantmentLevelValue {
        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:levels_squared");
            json.addProperty("added", added);
            return json;
        }
    }

    record Linear(float base, float perLevelAboveFirst) implements EnchantmentLevelValue {
        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:linear");
            json.addProperty("base", base);
            json.addProperty("per_level_above_first", perLevelAboveFirst);
            return json;
        }
    }

    record Lookup(List<Float> values, EnchantmentLevelValue fallback) implements EnchantmentLevelValue {
        public Lookup {
            values = List.copyOf(values);
            fallback = Objects.requireNonNull(fallback, "fallback");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:lookup");
            var array = new JsonArray();
            for (float value : values) {
                array.add(value);
            }
            json.add("values", array);
            json.add("fallback", fallback.toJson());
            return json;
        }
    }

    record Raw(JsonObject value) implements EnchantmentLevelValue {
        public Raw {
            value = EnchantmentJson.rawObject(value);
        }

        @Override
        public JsonObject toJson() {
            return value.deepCopy();
        }
    }
}
