package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.fand.api.VanillaKey;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/** Typed holder-set value used by registry-backed vanilla item components. */
public final class ItemKeySet implements ItemComponentData {

    public static final ItemKeySet EMPTY = new ItemKeySet(null, List.of());

    private final @Nullable Key tag;
    private final List<Key> values;

    public ItemKeySet(@Nullable Key tag, List<Key> values) {
        this.tag = tag;
        this.values = List.copyOf(Objects.requireNonNull(values, "values"));
        if (tag != null && !values.isEmpty()) {
            throw new IllegalArgumentException("ItemKeySet must contain either a tag or values, not both");
        }
    }

    public static ItemKeySet of(Key value) {
        return new ItemKeySet(null, List.of(Objects.requireNonNull(value, "value")));
    }

    public static ItemKeySet of(VanillaKey value) {
        Objects.requireNonNull(value, "value");
        return of(value.key());
    }

    public static ItemKeySet of(VanillaKey first, VanillaKey... rest) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(rest, "rest");
        var values = new java.util.ArrayList<Key>(1 + rest.length);
        values.add(first.key());
        for (var value : rest) {
            values.add(Objects.requireNonNull(value, "value").key());
        }
        return of(values);
    }

    public static ItemKeySet of(List<Key> values) {
        return new ItemKeySet(null, values);
    }

    public static ItemKeySet tag(Key tag) {
        return new ItemKeySet(Objects.requireNonNull(tag, "tag"), List.of());
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

    public boolean tagReference() {
        return tag != null;
    }

    public boolean empty() {
        return tag == null && values.isEmpty();
    }

    public Optional<Key> tag() {
        return Optional.ofNullable(tag);
    }

    public List<Key> values() {
        return values;
    }

    @Override
    public JsonElement toJson() {
        if (tag != null) {
            return new JsonPrimitive("#" + tag.asString());
        }
        if (values.size() == 1) {
            return new JsonPrimitive(values.getFirst().asString());
        }
        var array = new JsonArray();
        values.forEach(value -> array.add(value.asString()));
        return array;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemKeySet that)) {
            return false;
        }
        return Objects.equals(tag, that.tag) && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, values);
    }

    @Override
    public String toString() {
        return "ItemKeySet[tag=" + tag() + ", values=" + values + "]";
    }
}
