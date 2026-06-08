package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/** Typed value for {@code minecraft:potion_contents}. */
public final class ItemPotionContents implements ItemComponentData {

    public static final ItemPotionContents EMPTY = new ItemPotionContents(null, null, List.of(), null);

    private final @Nullable Key potion;
    private final @Nullable Integer customColor;
    private final List<ItemEffectInstance> customEffects;
    private final @Nullable String customName;

    public ItemPotionContents(
            @Nullable Key potion,
            @Nullable Integer customColor,
            List<ItemEffectInstance> customEffects,
            @Nullable String customName) {
        this.potion = potion;
        this.customColor = customColor;
        this.customEffects = List.copyOf(Objects.requireNonNull(customEffects, "customEffects"));
        this.customName = customName;
    }

    public ItemPotionContents(Key potion) {
        this(Objects.requireNonNull(potion, "potion"), null, List.of(), null);
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
                ItemComponentJson.optionalKey(object, "potion").orElse(null),
                ItemComponentJson.optionalInt(object, "custom_color").orElse(null),
                effects,
                ItemComponentJson.optionalString(object, "custom_name").orElse(null));
    }

    public Optional<Key> potion() {
        return Optional.ofNullable(potion);
    }

    public Optional<Integer> customColor() {
        return Optional.ofNullable(customColor);
    }

    public List<ItemEffectInstance> customEffects() {
        return customEffects;
    }

    public Optional<String> customName() {
        return Optional.ofNullable(customName);
    }

    @Override
    public JsonElement toJson() {
        if (potion != null && customColor == null && customEffects.isEmpty() && customName == null) {
            return new com.google.gson.JsonPrimitive(potion.asString());
        }
        var json = new JsonObject();
        if (potion != null) {
            json.addProperty("potion", potion.asString());
        }
        if (customColor != null) {
            json.addProperty("custom_color", customColor);
        }
        if (!customEffects.isEmpty()) {
            var effects = new JsonArray();
            customEffects.forEach(effect -> effects.add(effect.toJson()));
            json.add("custom_effects", effects);
        }
        if (customName != null) {
            json.addProperty("custom_name", customName);
        }
        return json;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemPotionContents that)) {
            return false;
        }
        return Objects.equals(potion, that.potion)
                && Objects.equals(customColor, that.customColor)
                && customEffects.equals(that.customEffects)
                && Objects.equals(customName, that.customName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(potion, customColor, customEffects, customName);
    }

    @Override
    public String toString() {
        return "ItemPotionContents[potion=" + potion()
                + ", customColor=" + customColor()
                + ", customEffects=" + customEffects
                + ", customName=" + customName()
                + "]";
    }
}
