package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:profile}. */
public record ItemProfile(
        Optional<String> name,
        Optional<UUID> id,
        List<ItemProfile.Property> properties,
        Optional<Key> texture,
        Optional<Key> cape,
        Optional<Key> elytra,
        Optional<String> model) implements ItemComponentData {

    public ItemProfile {
        name = Objects.requireNonNull(name, "name");
        id = Objects.requireNonNull(id, "id");
        properties = List.copyOf(Objects.requireNonNull(properties, "properties"));
        texture = Objects.requireNonNull(texture, "texture");
        cape = Objects.requireNonNull(cape, "cape");
        elytra = Objects.requireNonNull(elytra, "elytra");
        model = Objects.requireNonNull(model, "model");
    }

    public static ItemProfile named(String name) {
        return new ItemProfile(Optional.of(name), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ItemProfile fromJson(JsonElement value) {
        Objects.requireNonNull(value, "value");
        if (value.isJsonPrimitive()) {
            return named(value.getAsString());
        }
        var object = ItemComponentJson.object(value, "profile");
        return new ItemProfile(
                ItemComponentJson.optionalString(object, "name"),
                ItemComponentJson.optionalString(object, "id").map(UUID::fromString),
                propertiesFromJson(object.get("properties")),
                ItemComponentJson.optionalKey(object, "texture"),
                ItemComponentJson.optionalKey(object, "cape"),
                ItemComponentJson.optionalKey(object, "elytra"),
                ItemComponentJson.optionalString(object, "model"));
    }

    @Override
    public JsonElement toJson() {
        if (name.isPresent() && id.isEmpty() && properties.isEmpty() && texture.isEmpty() && cape.isEmpty() && elytra.isEmpty() && model.isEmpty()) {
            return new JsonPrimitive(name.orElseThrow());
        }
        var json = new JsonObject();
        name.ifPresent(value -> json.addProperty("name", value));
        id.ifPresent(value -> json.addProperty("id", value.toString()));
        if (!properties.isEmpty()) {
            var propertyArray = new com.google.gson.JsonArray();
            properties.forEach(property -> propertyArray.add(property.toJson()));
            json.add("properties", propertyArray);
        }
        texture.ifPresent(value -> json.addProperty("texture", value.asString()));
        cape.ifPresent(value -> json.addProperty("cape", value.asString()));
        elytra.ifPresent(value -> json.addProperty("elytra", value.asString()));
        model.ifPresent(value -> json.addProperty("model", value));
        return json;
    }

    private static List<Property> propertiesFromJson(JsonElement value) {
        if (value == null) {
            return List.of();
        }
        var properties = new ArrayList<Property>();
        if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(property -> properties.add(Property.fromJson(property)));
            return List.copyOf(properties);
        }
        if (!value.isJsonObject()) {
            return List.of();
        }
        for (var entry : value.getAsJsonObject().entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                properties.add(Property.unsigned(entry.getKey(), entry.getValue().getAsString()));
            } else if (entry.getValue().isJsonArray()) {
                entry.getValue().getAsJsonArray().forEach(property -> {
                    if (property.isJsonPrimitive()) {
                        properties.add(Property.unsigned(entry.getKey(), property.getAsString()));
                    }
                });
            }
        }
        return List.copyOf(properties);
    }

    /** Mojang profile property entry, usually used for signed skin textures. */
    public record Property(String name, String value, Optional<String> signature) implements ItemComponentData {

        public Property {
            name = Objects.requireNonNull(name, "name");
            value = Objects.requireNonNull(value, "value");
            signature = Objects.requireNonNull(signature, "signature");
        }

        public static Property unsigned(String name, String value) {
            return new Property(name, value, Optional.empty());
        }

        public static Property signed(String name, String value, String signature) {
            return new Property(name, value, Optional.of(signature));
        }

        public static Property fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "profile property");
            return new Property(
                    ItemComponentJson.optionalString(object, "name").orElseThrow(() -> new IllegalArgumentException("profile property name is required")),
                    ItemComponentJson.optionalString(object, "value").orElseThrow(() -> new IllegalArgumentException("profile property value is required")),
                    ItemComponentJson.optionalString(object, "signature"));
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("name", name);
            json.addProperty("value", value);
            signature.ifPresent(value -> json.addProperty("signature", value));
            return json;
        }
    }
}
