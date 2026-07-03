package io.fand.server.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.fand.api.component.DataComponentMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Saved-data backing store for Fand persistent block/entity components.
 */
public final class PersistentComponentData extends SavedData {

    private static final Codec<Map<String, String>> COMPONENT_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING);
    public static final Codec<PersistentComponentData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(Codec.STRING, COMPONENT_CODEC)
                            .optionalFieldOf("values", Map.of())
                            .forGetter(PersistentComponentData::serialized))
            .apply(instance, PersistentComponentData::new));

    public static SavedDataType<PersistentComponentData> blockType() {
        return new SavedDataType<>(
                Identifier.fromNamespaceAndPath("fand", "components/blocks"),
                PersistentComponentData::new,
                CODEC,
                DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }

    public static SavedDataType<PersistentComponentData> entityType() {
        return new SavedDataType<>(
                Identifier.fromNamespaceAndPath("fand", "components/entities"),
                PersistentComponentData::new,
                CODEC,
                DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }

    public static SavedDataType<PersistentComponentData> advancementType() {
        return new SavedDataType<>(
                Identifier.fromNamespaceAndPath("fand", "components/advancements"),
                PersistentComponentData::new,
                CODEC,
                DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }

    private final Map<String, Map<String, String>> values;
    private final Map<String, Set<String>> componentIndex = new HashMap<>();

    public PersistentComponentData() {
        this(Map.of());
    }

    public PersistentComponentData(Map<String, Map<String, String>> values) {
        this.values = new HashMap<>();
        values.forEach((id, components) -> {
            if (!components.isEmpty()) {
                var copy = new HashMap<>(components);
                this.values.put(id, copy);
                index(id, copy.keySet());
            }
        });
    }

    public DataComponentMap get(String id) {
        var stored = values.get(id);
        if (stored == null || stored.isEmpty()) {
            return DataComponentMap.EMPTY;
        }
        var decoded = new LinkedHashMap<Key, JsonElement>();
        stored.forEach((key, json) -> decoded.put(Key.key(key), JsonParser.parseString(json)));
        return new DataComponentMap(decoded);
    }

    public boolean empty(String id) {
        var stored = values.get(id);
        return stored == null || stored.isEmpty();
    }

    public Map<String, DataComponentMap> entries() {
        var entries = new LinkedHashMap<String, DataComponentMap>();
        values.keySet().stream().sorted().forEach(id -> entries.put(id, get(id)));
        return Collections.unmodifiableMap(entries);
    }

    public Set<String> idsWith(Key componentKey) {
        var ids = componentIndex.get(componentKey.asString());
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        java.util.LinkedHashSet<String> sorted = ids.stream()
                .sorted()
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        return Collections.unmodifiableSet(sorted);
    }

    public void put(String id, DataComponentMap components) {
        if (components.isEmpty()) {
            clear(id);
            return;
        }
        var encoded = new HashMap<String, String>();
        components.values().forEach((key, value) -> encoded.put(key.asString(), value.toString()));
        unindex(id);
        values.put(id, encoded);
        index(id, encoded.keySet());
        setDirty();
    }

    public void clear(String id) {
        if (values.remove(id) != null) {
            unindex(id);
            setDirty();
        }
    }

    private void index(String id, Iterable<String> keys) {
        for (var key : keys) {
            componentIndex.computeIfAbsent(key, ignored -> new java.util.LinkedHashSet<>()).add(id);
        }
    }

    private void unindex(String id) {
        var emptyKeys = new java.util.ArrayList<String>();
        componentIndex.forEach((key, ids) -> {
            ids.remove(id);
            if (ids.isEmpty()) {
                emptyKeys.add(key);
            }
        });
        emptyKeys.forEach(componentIndex::remove);
    }

    private Map<String, Map<String, String>> serialized() {
        var copy = new HashMap<String, Map<String, String>>();
        values.forEach((id, components) -> copy.put(id, Map.copyOf(components)));
        return copy;
    }
}
