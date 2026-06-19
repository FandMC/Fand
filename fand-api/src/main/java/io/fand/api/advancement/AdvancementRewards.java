package io.fand.api.advancement;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public record AdvancementRewards(
        int experience,
        List<Key> loot,
        List<Key> recipes,
        Optional<Key> function
) {

    public static final AdvancementRewards EMPTY = new AdvancementRewards(0, List.of(), List.of(), Optional.empty());

    public AdvancementRewards {
        if (experience < 0) {
            throw new IllegalArgumentException("experience must be >= 0");
        }
        Objects.requireNonNull(loot, "loot");
        Objects.requireNonNull(recipes, "recipes");
        loot = List.copyOf(loot);
        recipes = List.copyOf(recipes);
        function = Objects.requireNonNull(function, "function");
    }

    public JsonObject toVanillaJson() {
        var json = new JsonObject();
        if (experience != 0) {
            json.addProperty("experience", experience);
        }
        if (!loot.isEmpty()) {
            json.add("loot", keyArray(loot));
        }
        if (!recipes.isEmpty()) {
            json.add("recipes", keyArray(recipes));
        }
        function.ifPresent(key -> json.addProperty("function", key.asString()));
        return json;
    }

    private static JsonArray keyArray(List<Key> keys) {
        var array = new JsonArray();
        keys.forEach(key -> array.add(key.asString()));
        return array;
    }
}
