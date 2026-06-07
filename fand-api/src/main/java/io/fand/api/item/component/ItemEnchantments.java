package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Immutable value for {@code minecraft:enchantments} and
 * {@code minecraft:stored_enchantments}.
 */
public record ItemEnchantments(Map<Key, Integer> levels) {

    public static final int MAX_LEVEL = 255;
    public static final ItemEnchantments EMPTY = new ItemEnchantments(Map.of());

    public ItemEnchantments {
        Objects.requireNonNull(levels, "levels");
        var copied = new LinkedHashMap<Key, Integer>();
        for (var entry : levels.entrySet()) {
            var key = Objects.requireNonNull(entry.getKey(), "enchantment key");
            int level = Objects.requireNonNull(entry.getValue(), "enchantment level");
            validateStoredLevel(level);
            copied.put(key, level);
        }
        levels = Collections.unmodifiableMap(copied);
    }

    public static ItemEnchantments empty() {
        return EMPTY;
    }

    public static ItemEnchantments of(Key enchantment, int level) {
        return EMPTY.with(enchantment, level);
    }

    public boolean isEmpty() {
        return levels.isEmpty();
    }

    public boolean has(Key enchantment) {
        return levels.containsKey(enchantment);
    }

    public int level(Key enchantment) {
        return levels.getOrDefault(enchantment, 0);
    }

    public ItemEnchantments with(Key enchantment, int level) {
        Objects.requireNonNull(enchantment, "enchantment");
        if (level <= 0) {
            return without(enchantment);
        }
        validateStoredLevel(level);
        var next = new LinkedHashMap<>(levels);
        next.put(enchantment, level);
        return new ItemEnchantments(next);
    }

    public ItemEnchantments upgrade(Key enchantment, int level) {
        Objects.requireNonNull(enchantment, "enchantment");
        if (level <= 0) {
            return this;
        }
        return with(enchantment, Math.max(level(enchantment), level));
    }

    public ItemEnchantments without(Key enchantment) {
        Objects.requireNonNull(enchantment, "enchantment");
        if (!levels.containsKey(enchantment)) {
            return this;
        }
        var next = new LinkedHashMap<>(levels);
        next.remove(enchantment);
        return new ItemEnchantments(next);
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        levels.forEach((key, level) -> json.add(key.asString(), new JsonPrimitive(level)));
        return json;
    }

    public static ItemEnchantments fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return EMPTY;
        }
        var levels = new LinkedHashMap<Key, Integer>();
        for (var entry : value.getAsJsonObject().entrySet()) {
            if (entry.getValue() != null && entry.getValue().isJsonPrimitive()) {
                int level = entry.getValue().getAsInt();
                if (level > 0) {
                    levels.put(Key.key(entry.getKey()), level);
                }
            }
        }
        return new ItemEnchantments(levels);
    }

    private static void validateStoredLevel(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("enchantment level must be in 1.." + MAX_LEVEL + ", got " + level);
        }
    }
}
