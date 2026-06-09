package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jspecify.annotations.Nullable;

/** Tooltip display mode for an item attribute modifier entry. */
public final class ItemAttributeModifierDisplay implements ItemComponentData {

    public static final ItemAttributeModifierDisplay DEFAULT = new ItemAttributeModifierDisplay(Type.DEFAULT, null);
    public static final ItemAttributeModifierDisplay HIDDEN = new ItemAttributeModifierDisplay(Type.HIDDEN, null);

    private final Type type;
    private final @Nullable Component value;

    public ItemAttributeModifierDisplay(Type type, @Nullable Component value) {
        this.type = Objects.requireNonNull(type, "type");
        this.value = value;
        if (type != Type.OVERRIDE && value != null) {
            throw new IllegalArgumentException("only override display may contain a component");
        }
        if (type == Type.OVERRIDE && value == null) {
            throw new IllegalArgumentException("override display requires a component");
        }
    }

    public static ItemAttributeModifierDisplay override(Component component) {
        return new ItemAttributeModifierDisplay(Type.OVERRIDE, Objects.requireNonNull(component, "component"));
    }

    public static ItemAttributeModifierDisplay fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "attribute modifier display");
        var type = Type.fromSerializedName(object.get("type").getAsString());
        return new ItemAttributeModifierDisplay(
                type,
                type == Type.OVERRIDE ? GsonComponentSerializer.gson().deserializeFromTree(object.get("value")) : null);
    }

    public Type type() {
        return type;
    }

    public Optional<Component> value() {
        return Optional.ofNullable(value);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("type", type.serializedName());
        if (type == Type.OVERRIDE) {
            json.add("value", GsonComponentSerializer.gson().serializeToTree(value));
        }
        return json;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemAttributeModifierDisplay that)) {
            return false;
        }
        return type == that.type && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return "ItemAttributeModifierDisplay[type=" + type + ", value=" + value() + "]";
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
