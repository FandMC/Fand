package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/** Typed value for {@code minecraft:pot_decorations}. */
public final class ItemPotDecorations implements ItemComponentData {

    public static final Key BRICK = Key.key("minecraft:brick");
    public static final ItemPotDecorations EMPTY = new ItemPotDecorations(null, null, null, null);

    private final @Nullable Key back;
    private final @Nullable Key left;
    private final @Nullable Key right;
    private final @Nullable Key front;

    public ItemPotDecorations(@Nullable Key back, @Nullable Key left, @Nullable Key right, @Nullable Key front) {
        this.back = back;
        this.left = left;
        this.right = right;
        this.front = front;
    }

    public static ItemPotDecorations fromJson(JsonElement value) {
        var keys = ItemComponentJson.keys(value);
        return new ItemPotDecorations(side(keys, 0), side(keys, 1), side(keys, 2), side(keys, 3));
    }

    public Optional<Key> back() {
        return Optional.ofNullable(back);
    }

    public Optional<Key> left() {
        return Optional.ofNullable(left);
    }

    public Optional<Key> right() {
        return Optional.ofNullable(right);
    }

    public Optional<Key> front() {
        return Optional.ofNullable(front);
    }

    @Override
    public JsonArray toJson() {
        var json = new JsonArray();
        ordered().forEach(key -> json.add(key.asString()));
        return json;
    }

    public List<Key> ordered() {
        return List.of(
                back == null ? BRICK : back,
                left == null ? BRICK : left,
                right == null ? BRICK : right,
                front == null ? BRICK : front);
    }

    private static @Nullable Key side(List<Key> keys, int index) {
        if (index >= keys.size() || keys.get(index).equals(BRICK)) {
            return null;
        }
        return keys.get(index);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemPotDecorations that)) {
            return false;
        }
        return Objects.equals(back, that.back)
                && Objects.equals(left, that.left)
                && Objects.equals(right, that.right)
                && Objects.equals(front, that.front);
    }

    @Override
    public int hashCode() {
        return Objects.hash(back, left, right, front);
    }

    @Override
    public String toString() {
        return "ItemPotDecorations[back=" + back()
                + ", left=" + left()
                + ", right=" + right()
                + ", front=" + front()
                + "]";
    }
}
