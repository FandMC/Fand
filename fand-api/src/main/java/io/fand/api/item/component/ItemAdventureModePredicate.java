package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.List;
import java.util.Objects;

/** Typed value for {@code minecraft:can_place_on} and {@code minecraft:can_break}. */
public record ItemAdventureModePredicate(List<ItemBlockPredicate> predicates) implements ItemComponentData {

    public static final ItemAdventureModePredicate EMPTY = new ItemAdventureModePredicate(List.of());

    public ItemAdventureModePredicate {
        predicates = List.copyOf(Objects.requireNonNull(predicates, "predicates"));
    }

    public static ItemAdventureModePredicate of(ItemBlockPredicate predicate) {
        return new ItemAdventureModePredicate(List.of(predicate));
    }

    public static ItemAdventureModePredicate fromJson(JsonElement value) {
        Objects.requireNonNull(value, "value");
        if (value.isJsonObject()) {
            return of(ItemBlockPredicate.fromJson(value));
        }
        if (!value.isJsonArray()) {
            throw new IllegalArgumentException("adventure mode predicate must be an object or array");
        }
        var predicates = new java.util.ArrayList<ItemBlockPredicate>();
        for (var entry : value.getAsJsonArray()) {
            predicates.add(ItemBlockPredicate.fromJson(entry));
        }
        return new ItemAdventureModePredicate(predicates);
    }

    @Override
    public JsonElement toJson() {
        if (predicates.size() == 1) {
            return predicates.getFirst().toJson();
        }
        var json = new JsonArray();
        predicates.forEach(predicate -> json.add(predicate.toJson()));
        return json;
    }
}
