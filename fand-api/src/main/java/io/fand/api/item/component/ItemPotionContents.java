package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:potion_contents}. */
public record ItemPotionContents(
        Optional<Key> potion,
        Optional<Integer> customColor,
        List<ItemEffectInstance> customEffects,
        Optional<String> customName) implements ItemComponentData {

    public static final ItemPotionContents EMPTY = new ItemPotionContents(Optional.empty(), Optional.empty(), List.of(), Optional.empty());

    public ItemPotionContents {
        potion = Objects.requireNonNull(potion, "potion");
        customColor = Objects.requireNonNull(customColor, "customColor");
        customEffects = List.copyOf(Objects.requireNonNull(customEffects, "customEffects"));
        customName = Objects.requireNonNull(customName, "customName");
    }

    public ItemPotionContents(Key potion) {
        this(Optional.of(potion), Optional.empty(), List.of(), Optional.empty());
    }

    public ItemPotionContents(PotionKey potion) {
        this(Objects.requireNonNull(potion, "potion").key());
    }

    public static ItemPotionContents fromJson(JsonElement value) {
        Objects.requireNonNull(value, "value");
        if (value.isJsonPrimitive()) {
            return new ItemPotionContents(Key.key(value.getAsString()));
        }
        var object = ItemComponentJson.object(value, "potion contents");
        var effects = new java.util.ArrayList<ItemEffectInstance>();
        var rawEffects = object.get("custom_effects");
        if (rawEffects != null && rawEffects.isJsonArray()) {
            for (var effect : rawEffects.getAsJsonArray()) {
                effects.add(ItemEffectInstance.fromJson(effect));
            }
        }
        return new ItemPotionContents(
                ItemComponentJson.optionalKey(object, "potion"),
                ItemComponentJson.optionalInt(object, "custom_color"),
                effects,
                ItemComponentJson.optionalString(object, "custom_name"));
    }

    @Override
    public JsonElement toJson() {
        if (potion.isPresent() && customColor.isEmpty() && customEffects.isEmpty() && customName.isEmpty()) {
            return new com.google.gson.JsonPrimitive(potion.orElseThrow().asString());
        }
        var json = new JsonObject();
        potion.ifPresent(value -> json.addProperty("potion", value.asString()));
        customColor.ifPresent(value -> json.addProperty("custom_color", value));
        if (!customEffects.isEmpty()) {
            var effects = new JsonArray();
            customEffects.forEach(effect -> effects.add(effect.toJson()));
            json.add("custom_effects", effects);
        }
        customName.ifPresent(value -> json.addProperty("custom_name", value));
        return json;
    }
}
