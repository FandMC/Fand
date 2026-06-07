package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;

/**
 * Immutable item data component patch.
 *
 * <p>Keys and JSON values follow Mojang's modern item component format. A
 * removed component is encoded with a leading {@code !} key in patch JSON, just
 * like vanilla item stack serialization.
 */
public record ItemComponents(Map<Key, JsonElement> values, Set<Key> removals) {

    public static final ItemComponents EMPTY = new ItemComponents(Map.of(), Set.of());

    public ItemComponents {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(removals, "removals");
        var copiedValues = new LinkedHashMap<Key, JsonElement>();
        for (var entry : values.entrySet()) {
            var key = Objects.requireNonNull(entry.getKey(), "component key");
            var value = Objects.requireNonNull(entry.getValue(), "component value");
            copiedValues.put(key, value.deepCopy());
        }
        var copiedRemovals = new LinkedHashSet<Key>();
        for (var key : removals) {
            if (!copiedValues.containsKey(key)) {
                copiedRemovals.add(Objects.requireNonNull(key, "removed component key"));
            }
        }
        values = Collections.unmodifiableMap(copiedValues);
        removals = Collections.unmodifiableSet(copiedRemovals);
    }

    public static ItemComponents empty() {
        return EMPTY;
    }

    @Override
    public Map<Key, JsonElement> values() {
        var copied = new LinkedHashMap<Key, JsonElement>();
        values.forEach((key, value) -> copied.put(key, value.deepCopy()));
        return Collections.unmodifiableMap(copied);
    }

    public static ItemComponents of(Key key, JsonElement value) {
        return EMPTY.with(key, value);
    }

    public static ItemComponents fromJsonPatch(String json) {
        return fromJsonPatch(JsonParser.parseString(json));
    }

    public static ItemComponents fromJsonPatch(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            throw new IllegalArgumentException("Item component patch must be a JSON object");
        }
        var values = new LinkedHashMap<Key, JsonElement>();
        var removals = new LinkedHashSet<Key>();
        for (var entry : json.getAsJsonObject().entrySet()) {
            var rawKey = entry.getKey();
            boolean removed = rawKey.startsWith("!");
            var key = Key.key(removed ? rawKey.substring(1) : rawKey);
            if (removed) {
                removals.add(key);
            } else {
                values.put(key, entry.getValue().deepCopy());
            }
        }
        return new ItemComponents(values, removals);
    }

    public boolean isEmpty() {
        return values.isEmpty() && removals.isEmpty();
    }

    public Set<Key> keys() {
        return values.keySet();
    }

    public Set<Key> touchedKeys() {
        var keys = new LinkedHashSet<Key>();
        keys.addAll(values.keySet());
        keys.addAll(removals);
        return Collections.unmodifiableSet(keys);
    }

    public boolean has(Key key) {
        return values.containsKey(key);
    }

    public boolean removes(Key key) {
        return removals.contains(key);
    }

    public Optional<JsonElement> get(Key key) {
        var value = values.get(key);
        return value == null ? Optional.empty() : Optional.of(value.deepCopy());
    }

    public ItemComponents with(Key key, JsonElement value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        var nextValues = new LinkedHashMap<>(values);
        var nextRemovals = new LinkedHashSet<>(removals);
        nextValues.put(key, value.deepCopy());
        nextRemovals.remove(key);
        return new ItemComponents(nextValues, nextRemovals);
    }

    public ItemComponents withUnit(Key key) {
        return with(key, new JsonObject());
    }

    public ItemComponents withBoolean(Key key, boolean value) {
        return with(key, new JsonPrimitive(value));
    }

    public ItemComponents withInt(Key key, int value) {
        return with(key, new JsonPrimitive(value));
    }

    public ItemComponents withFloat(Key key, float value) {
        return with(key, new JsonPrimitive(value));
    }

    public ItemComponents withString(Key key, String value) {
        Objects.requireNonNull(value, "value");
        return with(key, new JsonPrimitive(value));
    }

    public ItemComponents withKey(Key key, Key value) {
        Objects.requireNonNull(value, "value");
        return withString(key, value.asString());
    }

    public ItemComponents withObject(Key key, JsonObject value) {
        return with(key, value);
    }

    public ItemComponents withArray(Key key, JsonArray value) {
        return with(key, value);
    }

    public ItemComponents without(Key key) {
        Objects.requireNonNull(key, "key");
        if (!values.containsKey(key) && !removals.contains(key)) {
            return this;
        }
        var nextValues = new LinkedHashMap<>(values);
        var nextRemovals = new LinkedHashSet<>(removals);
        nextValues.remove(key);
        nextRemovals.remove(key);
        return new ItemComponents(nextValues, nextRemovals);
    }

    public ItemComponents remove(Key key) {
        Objects.requireNonNull(key, "key");
        var nextValues = new LinkedHashMap<>(values);
        var nextRemovals = new LinkedHashSet<>(removals);
        nextValues.remove(key);
        nextRemovals.add(key);
        return new ItemComponents(nextValues, nextRemovals);
    }

    public ItemComponents removeAll(Collection<Key> keys) {
        Objects.requireNonNull(keys, "keys");
        var next = this;
        for (var key : keys) {
            next = next.remove(key);
        }
        return next;
    }

    public ItemComponents apply(ItemComponents patch) {
        Objects.requireNonNull(patch, "patch");
        var nextValues = new LinkedHashMap<>(values);
        var nextRemovals = new LinkedHashSet<>(removals);
        for (var key : patch.removals) {
            nextValues.remove(key);
            nextRemovals.add(key);
        }
        for (var entry : patch.values.entrySet()) {
            nextValues.put(entry.getKey(), entry.getValue().deepCopy());
            nextRemovals.remove(entry.getKey());
        }
        return new ItemComponents(nextValues, nextRemovals);
    }

    public JsonObject toJsonPatch() {
        var json = new JsonObject();
        for (var entry : values.entrySet()) {
            json.add(entry.getKey().asString(), entry.getValue().deepCopy());
        }
        for (var key : removals) {
            json.add("!" + key.asString(), new JsonObject());
        }
        return json;
    }
}
