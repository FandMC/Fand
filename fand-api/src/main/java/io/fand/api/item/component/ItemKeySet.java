package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Typed holder-set value used by registry-backed vanilla item components. */
public record ItemKeySet(Optional<Key> tag, List<Key> values) implements ItemComponentData {

    public static final ItemKeySet EMPTY = new ItemKeySet(Optional.empty(), List.of());

    public ItemKeySet {
        tag = Objects.requireNonNull(tag, "tag");
        values = List.copyOf(Objects.requireNonNull(values, "values"));
        if (tag.isPresent() && !values.isEmpty()) {
            throw new IllegalArgumentException("ItemKeySet must contain either a tag or values, not both");
        }
    }

    public static ItemKeySet of(Key value) {
        return new ItemKeySet(Optional.empty(), List.of(Objects.requireNonNull(value, "value")));
    }

    public static ItemKeySet of(List<Key> values) {
        return new ItemKeySet(Optional.empty(), values);
    }

    public static ItemKeySet tag(Key tag) {
        return new ItemKeySet(Optional.of(Objects.requireNonNull(tag, "tag")), List.of());
    }

    public static ItemKeySet fromJson(JsonElement value) {
        Objects.requireNonNull(value, "value");
        if (value.isJsonPrimitive()) {
            var text = value.getAsString();
            return text.startsWith("#") ? tag(Key.key(text.substring(1))) : of(Key.key(text));
        }
        if (!value.isJsonArray()) {
            throw new IllegalArgumentException("key set must be a string, tag string, or array");
        }
        var values = new java.util.ArrayList<Key>();
        for (var element : value.getAsJsonArray()) {
            if (!element.isJsonPrimitive()) {
                throw new IllegalArgumentException("key set array entries must be strings");
            }
            values.add(Key.key(element.getAsString()));
        }
        return of(values);
    }

    public boolean isTag() {
        return tag.isPresent();
    }

    public boolean isEmpty() {
        return tag.isEmpty() && values.isEmpty();
    }

    @Override
    public JsonElement toJson() {
        if (tag.isPresent()) {
            return new JsonPrimitive("#" + tag.orElseThrow().asString());
        }
        if (values.size() == 1) {
            return new JsonPrimitive(values.getFirst().asString());
        }
        var array = new JsonArray();
        values.forEach(value -> array.add(value.asString()));
        return array;
    }
}
