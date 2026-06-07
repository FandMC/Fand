package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/** Tooltip display mode for an item attribute modifier entry. */
public record ItemAttributeModifierDisplay(Type type, Optional<Component> value) implements ItemComponentData {

    public static final ItemAttributeModifierDisplay DEFAULT = new ItemAttributeModifierDisplay(Type.DEFAULT, Optional.empty());
    public static final ItemAttributeModifierDisplay HIDDEN = new ItemAttributeModifierDisplay(Type.HIDDEN, Optional.empty());

    public ItemAttributeModifierDisplay {
        type = Objects.requireNonNull(type, "type");
        value = Objects.requireNonNull(value, "value");
        if (type != Type.OVERRIDE && value.isPresent()) {
            throw new IllegalArgumentException("only override display may contain a component");
        }
        if (type == Type.OVERRIDE && value.isEmpty()) {
            throw new IllegalArgumentException("override display requires a component");
        }
    }

    public static ItemAttributeModifierDisplay override(Component component) {
        return new ItemAttributeModifierDisplay(Type.OVERRIDE, Optional.of(component));
    }

    public static ItemAttributeModifierDisplay fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "attribute modifier display");
        var type = Type.fromSerializedName(object.get("type").getAsString());
        return new ItemAttributeModifierDisplay(
                type,
                type == Type.OVERRIDE
                        ? Optional.of(GsonComponentSerializer.gson().deserializeFromTree(object.get("value")))
                        : Optional.empty());
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("type", type.serializedName());
        if (type == Type.OVERRIDE) {
            json.add("value", GsonComponentSerializer.gson().serializeToTree(value.orElseThrow()));
        }
        return json;
    }

    public enum Type {
        DEFAULT,
        HIDDEN,
        OVERRIDE;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Type fromSerializedName(String value) {
            return Type.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }
}
