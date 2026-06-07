package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:suspicious_stew_effects}. */
public record ItemSuspiciousStewEffects(List<ItemSuspiciousStewEffects.Entry> effects) implements ItemComponentData {

    public static final ItemSuspiciousStewEffects EMPTY = new ItemSuspiciousStewEffects(List.of());

    public ItemSuspiciousStewEffects {
        effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
    }

    public static ItemSuspiciousStewEffects fromJson(JsonElement value) {
        if (value == null || !value.isJsonArray()) {
            return EMPTY;
        }
        var effects = new java.util.ArrayList<Entry>();
        for (var entry : value.getAsJsonArray()) {
            effects.add(Entry.fromJson(entry));
        }
        return new ItemSuspiciousStewEffects(effects);
    }

    @Override
    public JsonArray toJson() {
        var json = new JsonArray();
        effects.forEach(effect -> json.add(effect.toJson()));
        return json;
    }

    public record Entry(Key effect, int duration) implements ItemComponentData {

        public Entry {
            effect = Objects.requireNonNull(effect, "effect");
        }

        public static Entry fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "suspicious stew effect");
            return new Entry(ItemComponentJson.key(object, "id"), ItemComponentJson.intOr(object, "duration", 160));
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("id", effect.asString());
            if (duration != 160) {
                json.addProperty("duration", duration);
            }
            return json;
        }
    }
}
