package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:trim}. */
public record ItemArmorTrim(Key material, Key pattern) implements ItemComponentData {

    public ItemArmorTrim {
        material = Objects.requireNonNull(material, "material");
        pattern = Objects.requireNonNull(pattern, "pattern");
    }

    public ItemArmorTrim(TrimMaterialKey material, TrimPatternKey pattern) {
        this(Objects.requireNonNull(material, "material").key(), Objects.requireNonNull(pattern, "pattern").key());
    }

    public static ItemArmorTrim fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "armor trim");
        return new ItemArmorTrim(ItemComponentJson.key(object, "material"), ItemComponentJson.key(object, "pattern"));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("material", material.asString());
        json.addProperty("pattern", pattern.asString());
        return json;
    }
}
