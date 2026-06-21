package io.fand.api.enchantment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

final class EnchantmentJson {

    private EnchantmentJson() {
    }

    static JsonObject object(String type) {
        var json = new JsonObject();
        json.addProperty("type", type);
        return json;
    }

    static JsonObject rawObject(JsonObject raw) {
        return Objects.requireNonNull(raw, "raw").deepCopy();
    }

    static JsonArray array(List<? extends EnchantmentJsonValue> values) {
        var array = new JsonArray();
        for (var value : values) {
            array.add(Objects.requireNonNull(value, "value").toJson());
        }
        return array;
    }

    static JsonArray strings(List<String> values) {
        var array = new JsonArray();
        for (var value : values) {
            array.add(Objects.requireNonNull(value, "value"));
        }
        return array;
    }

    static JsonArray keys(List<Key> values) {
        var array = new JsonArray();
        for (var value : values) {
            array.add(Objects.requireNonNull(value, "value").asString());
        }
        return array;
    }

    static JsonElement copy(JsonElement element) {
        return Objects.requireNonNull(element, "element").deepCopy();
    }
}
