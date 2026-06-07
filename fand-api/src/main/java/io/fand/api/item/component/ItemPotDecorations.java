package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:pot_decorations}. */
public record ItemPotDecorations(Optional<Key> back, Optional<Key> left, Optional<Key> right, Optional<Key> front) implements ItemComponentData {

    public static final Key BRICK = Key.key("minecraft:brick");
    public static final ItemPotDecorations EMPTY = new ItemPotDecorations(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public ItemPotDecorations {
        back = Objects.requireNonNull(back, "back");
        left = Objects.requireNonNull(left, "left");
        right = Objects.requireNonNull(right, "right");
        front = Objects.requireNonNull(front, "front");
    }

    public static ItemPotDecorations fromJson(JsonElement value) {
        var keys = ItemComponentJson.keys(value);
        return new ItemPotDecorations(side(keys, 0), side(keys, 1), side(keys, 2), side(keys, 3));
    }

    @Override
    public JsonArray toJson() {
        var json = new JsonArray();
        ordered().forEach(key -> json.add(key.asString()));
        return json;
    }

    public List<Key> ordered() {
        return List.of(back.orElse(BRICK), left.orElse(BRICK), right.orElse(BRICK), front.orElse(BRICK));
    }

    private static Optional<Key> side(List<Key> keys, int index) {
        if (index >= keys.size() || keys.get(index).equals(BRICK)) {
            return Optional.empty();
        }
        return Optional.of(keys.get(index));
    }
}
