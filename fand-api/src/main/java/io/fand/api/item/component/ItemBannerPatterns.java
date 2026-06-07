package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:banner_patterns}. */
public record ItemBannerPatterns(List<ItemBannerPatterns.Layer> layers) implements ItemComponentData {

    public static final ItemBannerPatterns EMPTY = new ItemBannerPatterns(List.of());

    public ItemBannerPatterns {
        layers = List.copyOf(Objects.requireNonNull(layers, "layers"));
    }

    public static ItemBannerPatterns fromJson(JsonElement value) {
        if (value == null || !value.isJsonArray()) {
            return EMPTY;
        }
        var layers = new java.util.ArrayList<Layer>();
        for (var layer : value.getAsJsonArray()) {
            layers.add(Layer.fromJson(layer));
        }
        return new ItemBannerPatterns(layers);
    }

    @Override
    public JsonArray toJson() {
        var json = new JsonArray();
        layers.forEach(layer -> json.add(layer.toJson()));
        return json;
    }

    public record Layer(Key pattern, ItemDyeColor color) implements ItemComponentData {

        public Layer {
            pattern = Objects.requireNonNull(pattern, "pattern");
            color = Objects.requireNonNull(color, "color");
        }

        public static Layer fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "banner pattern layer");
            return new Layer(ItemComponentJson.key(object, "pattern"), ItemDyeColor.fromSerializedName(object.get("color").getAsString()));
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("pattern", pattern.asString());
            json.addProperty("color", color.serializedName());
            return json;
        }
    }
}
