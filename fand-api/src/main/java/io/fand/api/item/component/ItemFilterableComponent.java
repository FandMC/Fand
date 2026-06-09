package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jspecify.annotations.Nullable;

/** Filterable Adventure component value used by written books. */
public final class ItemFilterableComponent implements ItemComponentData {

    private final Component raw;
    private final @Nullable Component filtered;

    public ItemFilterableComponent(Component raw, @Nullable Component filtered) {
        this.raw = Objects.requireNonNull(raw, "raw");
        this.filtered = filtered;
    }

    public static ItemFilterableComponent of(Component raw) {
        return new ItemFilterableComponent(raw, null);
    }

    public static ItemFilterableComponent fromJson(JsonElement value) {
        Objects.requireNonNull(value, "value");
        if (!value.isJsonObject() || !value.getAsJsonObject().has("raw")) {
            return of(deserialize(value));
        }
        var object = value.getAsJsonObject();
        return new ItemFilterableComponent(
                deserialize(object.get("raw")),
                object.has("filtered") ? deserialize(object.get("filtered")) : null);
    }

    public Component raw() {
        return raw;
    }

    public Optional<Component> filtered() {
        return Optional.ofNullable(filtered);
    }

    @Override
    public JsonElement toJson() {
        if (filtered == null) {
            return serialize(raw);
        }
        var json = new JsonObject();
        json.add("raw", serialize(raw));
        json.add("filtered", serialize(filtered));
        return json;
    }

    private static JsonElement serialize(Component component) {
        return GsonComponentSerializer.gson().serializeToTree(component);
    }

    private static Component deserialize(JsonElement component) {
        return GsonComponentSerializer.gson().deserializeFromTree(component);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemFilterableComponent that)) {
            return false;
        }
        return raw.equals(that.raw) && Objects.equals(filtered, that.filtered);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, filtered);
    }

    @Override
    public String toString() {
        return "ItemFilterableComponent[raw=" + raw + ", filtered=" + filtered() + "]";
    }
}
