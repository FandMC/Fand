package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;

/** Typed value for {@code minecraft:firework_explosion}. */
public record ItemFireworkExplosion(
        ItemFireworkShape shape,
        List<Integer> colors,
        List<Integer> fadeColors,
        boolean hasTrail,
        boolean hasTwinkle) implements ItemComponentData {

    public static final ItemFireworkExplosion DEFAULT =
            new ItemFireworkExplosion(ItemFireworkShape.SMALL_BALL, List.of(), List.of(), false, false);

    public ItemFireworkExplosion {
        shape = Objects.requireNonNull(shape, "shape");
        colors = List.copyOf(Objects.requireNonNull(colors, "colors"));
        fadeColors = List.copyOf(Objects.requireNonNull(fadeColors, "fadeColors"));
    }

    public static ItemFireworkExplosion fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return DEFAULT;
        }
        var object = value.getAsJsonObject();
        return new ItemFireworkExplosion(
                object.has("shape") ? ItemFireworkShape.fromSerializedName(object.get("shape").getAsString()) : ItemFireworkShape.SMALL_BALL,
                ints(object.get("colors")),
                ints(object.get("fade_colors")),
                object.has("has_trail") && object.get("has_trail").getAsBoolean(),
                object.has("has_twinkle") && object.get("has_twinkle").getAsBoolean());
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("shape", shape.serializedName());
        if (!colors.isEmpty()) {
            json.add("colors", intArray(colors));
        }
        if (!fadeColors.isEmpty()) {
            json.add("fade_colors", intArray(fadeColors));
        }
        if (hasTrail) {
            json.addProperty("has_trail", true);
        }
        if (hasTwinkle) {
            json.addProperty("has_twinkle", true);
        }
        return json;
    }

    private static JsonArray intArray(List<Integer> values) {
        var array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static List<Integer> ints(JsonElement value) {
        if (value == null || !value.isJsonArray()) {
            return List.of();
        }
        return value.getAsJsonArray().asList().stream()
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsInt)
                .toList();
    }
}
